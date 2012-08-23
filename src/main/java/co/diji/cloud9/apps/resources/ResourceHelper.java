package co.diji.cloud9.apps.resources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;

import org.mozilla.javascript.Script;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import co.diji.cloud9.exceptions.resources.ResourceException;
import co.diji.cloud9.services.HazelcastService;

@Component
public class ResourceHelper implements EntryListener<String, Resource> {

    private static final XLogger logger = XLoggerFactory.getXLogger(ResourceHelper.class);

    protected static final Set<String> STATIC_RESOURCES = new HashSet<String>(Arrays.asList(new String[]{
            "css", "images", "js", "html"}));

    protected Map<String, Script> scriptCache;
    protected IMap<String, Resource> resourceCache;

    @Autowired
    protected HazelcastService hazelcast;

    @Autowired
    protected ApplicationContext appContext;

    /**
     * Our bootstrap code which is responsible for hooking us up to the cache events
     */
    @PostConstruct
    protected void bootstrap() {
        logger.trace("in bootstrap");
        logger.debug("registering event listener");
        ((IMap<String, Resource>) getResourceCache()).addEntryListener(this, false);
        logger.trace("exit bootstrap");
    }

    /**
     * Gets the resource cache
     * 
     * @return the cache
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Resource> getResourceCache() {
        if (resourceCache == null) {
            resourceCache = (IMap<String, Resource>) hazelcast.getMap("resources");
        }

        return resourceCache;
    }

    /**
     * Gets the script cache
     * 
     * @return the cache
     */
    protected Map<String, Script> getScriptCache() {
        if (scriptCache == null) {
            scriptCache = new ConcurrentHashMap<String, Script>();
        }

        return scriptCache;
    }

    /**
     * Generates a cache key based on the app, dir and resource
     * 
     * @param app the app name of the resource
     * @param dir the dir/type name of the resource
     * @param resource the resource name/id
     * @return the generated cache key
     */
    protected String getCacheKey(String app, String dir, String resource) {
        logger.trace("in getCacheKey app:{}, dir:{}, resource:{}", new Object[]{app, dir, resource});
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(app);
        if (dir != null) {
            keyBuilder.append(dir);
            if (resource != null) {
                keyBuilder.append(resource);
            }
        }

        logger.trace("exit getCacheKey");
        return keyBuilder.toString();
    }

    /**
     * Responsible for retrieving the specific resource. Attempts to get from cache first, if not found, it creates new instance.
     * 
     * @param app the app the resource belongs to
     * @param dir the dir/type of the resource
     * @param resource the resource name/id
     * @return the resource
     * @throws ResourceException on error retrieving/creating the resource
     */
    public Resource getResource(String app, String dir, String resource) throws ResourceException {
        logger.trace("in getResource app:{} dir:{} resource:{}", new Object[]{app, dir, resource});

        Resource r;

        if (dir == null && resource == null) {
            // only given app name, go to index html by default
            logger.debug("no dir or resource, using html/index.html");
            dir = "html";
            resource = "index.html";
        } else if (resource == null) {
            // only app name and resource name which spring thinks is actually the dir
            // set the resource name and dir to html
            logger.debug("no resource, using html/{}", dir);
            resource = dir + ".html";
            dir = "html";
        }

        // if this is a static resource
        boolean isStatic = STATIC_RESOURCES.contains(dir);
        logger.debug("isStatic: {}", isStatic);
        if (!isStatic) {
            // javascript controllers are in the "controllers" type/dir and have resource name of dir + .js
            logger.debug("found javascript controller, using controllers/{}.js", dir);
            resource = dir + ".js";
            dir = "controllers";
        }

        // see if the resource is cached
        String cacheKey = getCacheKey(app, dir, resource);
        logger.debug("cacheKey: {}", cacheKey);
        r = getResourceCache().get(cacheKey);

        if (r == null) {
            // resource is not cached
            logger.debug("resource not cached");

            // have spring initialize the resource
            r = appContext.getBean(isStatic ? StaticResource.class : JavascriptResource.class);

            // run resource setup code
            r.setup(app, dir, resource);

            // add to cache
            logger.debug("adding resoruce to cache");
            getResourceCache().put(cacheKey, r);

            // if this is javascript resource, we add the compiled script to a local cache
            if (!isStatic) {
                getScriptCache().put(cacheKey, ((JavascriptResource) r).getScript());
            }
        } else {
            logger.debug("using cached resource");

            // if this is javascript resource, see if we have script cached.
            if (!isStatic) {
                logger.debug("found javascript resource, checking for cached script");
                JavascriptResource jr = (JavascriptResource) r;
                Script cachedScript = getScriptCache().get(cacheKey);
                if (cachedScript != null) {
                    // found cached script
                    logger.debug("found cached script");
                    jr.setScript(cachedScript);
                } else {
                    // script was not cached due to being added/updated on remote node
                    // get the script from the compiled script and cache it
                    logger.debug("cached script not found, adding to cache");
                    getScriptCache().put(cacheKey, jr.getScript());
                }
            }
        }

        logger.trace("exit getResource");
        return r;
    }

    /**
     * Evict resource from cache
     * 
     * @param app the app name the resource belongs to
     * @param dir the dir/type of the resource
     * @param resource the resource name/id
     */
    public void evict(String app, String dir, String resource) {
        logger.debug("in evict app:{}, dir:{}, resource:{}", new Object[]{app, dir, resource});
        evictByKey(getCacheKey(app, dir, resource));
    }

    /**
     * Evict all resources in the specified app with the specified dir/type
     * 
     * @param app the app name you the resources belong to
     * @param dir the dir/type of the resources you want evicted
     */
    public void evict(String app, String dir) {
        evictByPrefix(getCacheKey(app, dir, null));
    }

    /**
     * Evict all resources belonging to the specified app
     * 
     * @param app the app name you want resources evicted from
     */
    public void evict(String app) {
        evictByPrefix(getCacheKey(app, null, null));
    }

    /**
     * Evict an item from the resource cache
     * 
     * @param cacheKey the key of the item to evict
     */
    protected void evictByKey(String cacheKey) {
        logger.debug("in evict cacheKey:{}", cacheKey);
        // just remove the entry from resoruce cache
        // this will trigger remove event, which in turn deletes from script cache if necessary
        getResourceCache().remove(cacheKey);
        logger.debug("exit evict");
    }

    /**
     * Evict items matching a specific prefix
     * 
     * @param keyPrefix the prefix of items to evict
     */
    protected void evictByPrefix(String keyPrefix) {
        logger.debug("in evictByPrefix keyPrefix:{}", keyPrefix);
        // loop though cache keys and evict if they match prefix
        for (String cacheKey : getResourceCache().keySet()) {
            if (cacheKey.startsWith(keyPrefix)) {
                logger.debug("key {} matches", cacheKey);
                evictByKey(cacheKey);
            }
        }
        logger.debug("exit evictByPrefix");
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryAdded(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryAdded(EntryEvent<String, Resource> event) {
        logger.trace("in entryAdded");
        // do nothing, script will be cached locally on first use
        logger.trace("exit entryAdded");
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryEvicted(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryEvicted(EntryEvent<String, Resource> event) {
        logger.trace("in entryEvicted");
        // blindly try to remove resource from local cache
        logger.debug("expiring cached script if exists: {}", event.getKey());
        getScriptCache().remove(event.getKey());
        logger.trace("exit entryEvicted");
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryRemoved(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryRemoved(EntryEvent<String, Resource> event) {
        logger.trace("in entryRemoved");
        // blindly try to remove resource from local cache
        logger.debug("expiring cached script if exists: {}", event.getKey());
        getScriptCache().remove(event.getKey());
        logger.trace("exit entryRemoved");
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryUpdated(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryUpdated(EntryEvent<String, Resource> event) {
        logger.trace("in entryUpdated");
        String eventNodeId = event.getMember().getUuid();
        logger.debug("Update happend on node: {}", eventNodeId);
        if (!eventNodeId.equals(hazelcast.getNodeId())) {
            logger.debug("expiring cached script if exists: {}", event.getKey());
            getScriptCache().remove(event.getKey());
        }
        logger.trace("exit entryUpdated");
    }

}

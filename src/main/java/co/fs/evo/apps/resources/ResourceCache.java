package co.fs.evo.apps.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.mozilla.javascript.Script;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.fs.evo.services.HazelcastService;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;

@Component
public class ResourceCache implements EntryListener<String, Resource> {
	
	private static final XLogger logger = XLoggerFactory.getXLogger(ResourceCache.class);
	
	protected Map<String, Script> scriptCache;
	
    @Autowired
    protected HazelcastService hazelcast;

    //@Autowired
    //protected ApplicationContext appContext;
    
    /**
     * Our bootstrap code which is responsible for hooking us up to the cache events
     */
    @PostConstruct
    protected void bootstrap() {
        logger.entry();
        hazelcast.registerResourceListener(this);
        logger.exit();
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
        logger.entry(app, dir, resource);
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(app);
        if (dir != null) {
            keyBuilder.append(dir);
            if (resource != null) {
                keyBuilder.append(resource);
            }
        }

        logger.exit(keyBuilder);
        return keyBuilder.toString();
    }

    /**
     * Evict resource from cache
     * 
     * @param app the app name the resource belongs to
     * @param dir the dir/type of the resource
     * @param resource the resource name/id
     */
    public void evict(String app, String dir, String resource) {
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
        logger.entry(cacheKey);
        // just remove the entry from resource cache
        // this will trigger remove event, which in turn deletes from script cache if necessary
        hazelcast.evictResource(cacheKey);
        logger.exit();
    }

    /**
     * Evict items matching a specific prefix
     * 
     * @param keyPrefix the prefix of items to evict
     */
    protected void evictByPrefix(String keyPrefix) {
        logger.entry(keyPrefix);
        // loop though cache keys and evict if they match prefix
        for (String cacheKey : hazelcast.getResourceKeys()) {
            if (cacheKey.startsWith(keyPrefix)) {
                logger.debug("key {} matches", cacheKey);
                evictByKey(cacheKey);
            }
        }
        logger.exit();
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryAdded(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryAdded(EntryEvent<String, Resource> event) {
        // do nothing, script will be cached locally on first use
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryEvicted(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryEvicted(EntryEvent<String, Resource> event) {
        logger.entry();
        // blindly try to remove resource from local cache
        logger.debug("expiring cached script if exists: {}", event.getKey());
        getScriptCache().remove(event.getKey());
        logger.exit();
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryRemoved(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryRemoved(EntryEvent<String, Resource> event) {
        logger.entry();
        // blindly try to remove resource from local cache
        logger.debug("expiring cached script if exists: {}", event.getKey());
        getScriptCache().remove(event.getKey());
        logger.exit();
    }

    /*
     * (non-Javadoc)
     * @see com.hazelcast.core.EntryListener#entryUpdated(com.hazelcast.core.EntryEvent)
     */
    @Override
    public void entryUpdated(EntryEvent<String, Resource> event) {
        logger.entry();
        String eventNodeId = event.getMember().getUuid();
        logger.debug("Update happend on node: {}", eventNodeId);
        if (!eventNodeId.equals(hazelcast.getNodeId())) {
            logger.debug("expiring cached script if exists: {}", event.getKey());
            getScriptCache().remove(event.getKey());
        }
        logger.exit();
    }
    
    public Resource getResource(String cacheKey) {
    	return hazelcast.getResource(cacheKey);
    }
    
    public void putResource(String cacheKey, Resource r) {
    	hazelcast.putResource(cacheKey, r);
    }

    public boolean resourceCacheEnabled() {
    	return hazelcast.resourceCacheEnabled();
    }
}

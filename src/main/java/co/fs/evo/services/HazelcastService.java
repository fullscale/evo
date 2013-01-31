package co.fs.evo.services;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spring.context.SpringManagedContext;
import com.hazelcast.web.WebFilter;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import co.fs.evo.apps.resources.Resource;

@Service
public class HazelcastService {

    private static final XLogger logger = XLoggerFactory.getXLogger(HazelcastService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ApplicationContext applicationContext;

    // our main hazelcast instance
    private HazelcastInstance hazelcast = null;

    // application caches
    private IMap<String, Resource> resourceCache = null;

    /**
     * Hazelcast initialization
     */
    @PostConstruct
    public void bootstrap() {
        logger.entry();

        // exit if hazelcast should not be started
        if (!configService.getHazelcastEnabled()) {
            logger.info("Hazelcast disabled");
            logger.exit();
            return;
        }

        logger.info("Initializing distributed in-memory data grid");
        // read default evo hazelcast settings
        Config conf = new ClasspathXmlConfig("hazelcast-evo.xml");

        // set our hazelcast instance name to evo node name
        conf.setInstanceName(configService.getNodeName());

        // hazelcast group name should be the same as our elasticsearch cluster name
        conf.getGroupConfig().setName(configService.getClusterName());

        // if unicast is enabled, do the same for hazelcast
        boolean useUnicast = configService.getUnicastEnabled();
        logger.debug("useUnicast: {}", useUnicast);
        if (useUnicast) {
            logger.debug("disabling multicast");
            conf.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            logger.debug("enabling tcp/ip (unicast)");
            TcpIpConfig tcp = conf.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            for (String host : configService.getUnicastHosts()) {
                logger.debug("adding member: {}", host);
                tcp.addMember(host);
            }
        }

        // make sure hazelcast starts on the "publish" address that elasticsearch nodes communicate on
        logger.debug("setting interface");
        conf.getNetworkConfig().getInterfaces().setEnabled(true).addInterface(searchService.getPublishAddress());

        // configure hazelcast to be "spring aware"
        logger.debug("enabling spring managed context");
        SpringManagedContext springContext = new SpringManagedContext();
        springContext.setApplicationContext(applicationContext);
        conf.setManagedContext(springContext);

        // start hazelcast
        logger.debug("starting hazelcast");
        hazelcast = Hazelcast.newHazelcastInstance(conf);
        logger.info("Data grid is online");

        // get our application caches
        if (configService.getResourceCacheEnabled()) {
            logger.debug("Enabling resource cache");
            resourceCache = hazelcast.getMap("resources");
        } else {
            logger.info("Resource cache disabled");
        }

        logger.exit();
    }

    /**
     * Get's the id of the current node.
     * 
     * @return the node id
     */
    public String getNodeId() {
        return hazelcast.getCluster().getLocalMember().getUuid();
    }

    /**
     * Generic cache put to be used for application specific puts. We use this so we don't need to do the checks for every cache.
     * 
     * @param cache the application cache we want to put the value in
     * @param key the cache key
     * @param value the cache value
     */
    private <K, V> void put(IMap<K, V> cache, K key, V value) {
        // cache will be null if hazelcast is disabled or the application specific cache is disabled
        if (cache != null) {
            logger.debug("caching {}", key);
            try {
            	cache.put(key, value);
            } catch (RuntimeException ex) {
            	logger.debug(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Generic cache evict to be used for application specific evicts. We use this so we don't need to do the checks for every
     * cache.
     * 
     * @param cache the application cache to perform evict on
     * @param key the cache key to evict
     */
    private <K, V> void evict(IMap<K, V> cache, K key) {
        // cache will be null if hazelcast is disabled or the application specific cache is disabled
        if (cache != null) {
            logger.debug("evicting: {}", key);
            try {
            	cache.remove(key);
            } catch (RuntimeException ex) {
            	logger.debug(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Generic cache get to be used for application specific gets. We use this so we don't need to do the checks for every cache.
     * 
     * @param cache the application cache to perform the get on
     * @param key the cache key to get the value for
     * @return the cached value, null if does not exist or caching disabled
     */
    private <K, V> V get(IMap<K, V> cache, K key) {
        V value = null;
        // cache will be null if hazelcast is disabled or the application specific cache is disabled
        if (cache != null) {
            logger.debug("getting: {}", key);
            try {
            	value = cache.get(key);
            } catch (IllegalStateException ex) {
            	logger.warn("Hazelcast Instance is not active!");
            	logger.debug(ex.getMessage(), ex);
            }
        }

        return value;
    }

    /**
     * Generic method to register an entry listener for the cache. We use this so we don't need to do the checks for every cache.
     * 
     * @param cache the cache to register the listener for
     * @param listener the listener to register with the cache
     * @param includeValue if events should include the entry value.
     */
    private <K, V> void registerListener(IMap<K, V> cache, EntryListener<K, V> listener, boolean includeValue) {
        // cache will be null if hazelcast is disabled or the application specific cache is disabled
        if (cache != null) {
            logger.debug("registering listener");
            cache.addEntryListener(listener, includeValue);
        }
    }

    /**
     * Generic method to get the keys of an application cache. We use this so we don't need to do the checks for every cache.
     * 
     * @param cache The cache to get the keys for
     * @return the set of keys for the cache, empty set when cache is disabled
     */
    private <K, V> Set<K> getCacheKeys(IMap<K, V> cache) {
        Set<K> keys = Collections.emptySet();
        if (cache != null) {
            logger.debug("getting cache keys");
            try {
            	keys = cache.keySet();
            } catch (RuntimeException ex) {
            	logger.debug(ex.getMessage(), ex);
            }
        }

        return keys;
    }

    /**
     * Put item into resource cache.
     * 
     * @param key the resource cache key
     * @param resource the resource to cache
     */
    public void putResource(String key, Resource resource) {
        put(resourceCache, key, resource);
    }

    /**
     * Evict a resource from cache.
     * 
     * @param key the resource key to evict
     */
    public void evictResource(String key) {
        evict(resourceCache, key);
    }

    /**
     * Get a resource from the cache
     * 
     * @param key the resource cache key to get
     * @return the resource value, null if not found
     */
    public Resource getResource(String key) {
        return get(resourceCache, key);
    }

    /**
     * Get's the resource cache keys
     * 
     * @return Set containing the resource keys
     */
    public Set<String> getResourceKeys() {
        return getCacheKeys(resourceCache);
    }

    /**
     * Register a resource cache listener.
     * 
     * @param listener the listener to register on the resource cache.
     */
    public void registerResourceListener(EntryListener<String, Resource> listener) {
        // we don't need resource values when an even occurs
        registerListener(resourceCache, listener, false);
    }

    /**
     * If the resource cache is enabled or not
     * 
     * @return true if enabled, false when disabled
     */
    public boolean resourceCacheEnabled() {
        // resource cache is null when hazelcast is disabled or resource caching is disabled
        return (resourceCache != null);
    }

    /**
     * Hazelcast shutdown
     */
    @PreDestroy
    public void shutdown() {
        logger.entry();
        if (configService.getHazelcastEnabled() && hazelcast != null) {
            logger.info("Shutting down distributed in-memory data grid");
            hazelcast.getLifecycleService().shutdown();
        }
        logger.exit();
    }

    /**
     * Bean that spring uses for the session cache filter proxy
     * 
     * @return the session cache filter
     */
    @Bean
    public WebFilter hazelcastWebFilter() {
        logger.entry();
        
        if (!configService.getHazelcastEnabled() || !configService.getSessionCacheEnabled()) {
            logger.exit();
            return null;
        }
        
        Properties sessionCacheConfig = new Properties();
        sessionCacheConfig.put("instance-name", hazelcast.getName()); // the name of our hazelcast instance
        sessionCacheConfig.put("map-name", "session-cache");
        sessionCacheConfig.put("sticky-session", "false");
        sessionCacheConfig.put("cookie-name", "evo-sid");
        sessionCacheConfig.put("shutdown-on-destroy", "false");
        logger.debug("sessionCacheConfig: {}", sessionCacheConfig);

        logger.exit();
        return new WebFilter(sessionCacheConfig);
    }
}
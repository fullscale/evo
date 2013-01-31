package co.fs.evo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Constants {
	
    public static final long MILLISECONDS_IN_YEAR = 31556952000L;
    
    // search service
    public static final String SYSTEM_INDEX = "sys";
    public static final String APP_INDEX = "app";
    public static final String[] INVALID_INDEX_NAMES = {"css", "js", "img", "partials", "lib"};
    public static final String[] VALID_TYPES = {"conf", "html", "css", "img", "js", "server-side", "partials", "lib"};

	public static final Set<String> STATIC_RESOURCES = Collections.unmodifiableSet(
	    		new HashSet<String>(Arrays.asList(new String[]{
	    				"css", "img", "js", "html", "partials", "lib"
	    		})));
	
    // system property keys
    public static final String PROPERTY_HOME_DIR = "evo.home";
    public static final String PROPERTY_HTTP_PORT = "evo.http.port";
    public static final String PROPERTY_HTTPS_PORT = "evo.https.port";
    public static final String PROPERTY_HTTPS_ENABLED = "evo.https.enabled";
    public static final String PROPERTY_HTTPS_KEYPASS = "evo.https.keypass";
    public static final String PROPERTY_HTTPS_KEYSTORE = "evo.https.keystore";
    public static final String PROPERTY_SPDY_ENABLED = "evo.spdy.enabled";
    public static final String PROPERTY_NODE_NAME = "evo.node.name";
    public static final String PROPERTY_CLUSTER_NAME = "evo.cluster.name";
    public static final String PROPERTY_HAZELCAST_ENABLED = "evo.hazelcast.enabled";
    public static final String PROPERTY_CACHE_RESOURCES = "evo.cache.resources";
    public static final String PROPERTY_CACHE_SESSIONS = "evo.cache.sessions";
    public static final String PROPERTY_GZIP_ENABLED = "evo.gzip.enabled";
    public static final String PROPERTY_COREPOOL_SIZE = "evo.thread.pool.core.size";
    public static final String PROPERTY_MAXPOOL_SIZE = "evo.thread.pool.max.size";
    public static final String PROPERTY_QUEUE_CAPACITY = "evo.thread.queue.capacity";
    public static final String PROPERTY_KEEPALIVE_SECONDS = "evo.thread.keepalive.seconds";

    // config settings keys
    public static final String SETTING_HOME_DIR = "home";
    public static final String SETTING_NODE_NAME = "node.name";
    public static final String SETTING_CLUSTER_NAME = "cluster.name";
    public static final String SETTING_HTTP_PORT = "network.http.port";
    public static final String SETTING_HTTP_MAXTHREADS = "network.http.maxthreads";
    public static final String SETTING_HTTP_SESSION_TIMEOUT = "network.http.session.timeout";
    public static final String SETTING_HTTP_REQUESTLOG_ENABLED = "network.http.requestlog.enabled";
    public static final String SETTING_HTTPS_PORT = "network.https.port";
    public static final String SETTING_HTTPS_ENABLED = "network.https.enabled";
    public static final String SETTING_HTTPS_KEYPASS = "network.https.keypass";
    public static final String SETTING_HTTPS_KEYSTORE = "network.https.keystore";
    public static final String SETTING_SPDY_ENABLED = "network.spdy.enabled";
    public static final String SETTING_UNICAST_HOSTS = "network.unicast.hosts";
    public static final String SETTING_UNICAST_ENABLED = "network.unicast.enabled";
    public static final String SETTING_MULTICAST_ENABLED = "network.multicast.enabled";
    public static final String SETTING_HAZELCAST_ENABLED = "hazelcast.enabled";
    public static final String SETTING_CACHE_RESOURCES_ENABLED = "cache.resources.enabled";
    public static final String SETTING_CACHE_SESSIONS_ENABLED = "cache.sessions.enabled";
    public static final String SETTING_GZIP_ENABLED = "gzip.enabled";
    public static final String SETTING_COREPOOL_SIZE = "thread.pool.core.size";
    public static final String SETTING_MAXPOOL_SIZE = "thread.pool.max.size";
    public static final String SETTING_QUEUE_CAPACITY = "thread.queue.capacity";
    public static final String SETTING_KEEPALIVE_SECONDS = "thread.keepalive.seconds";

    // resource prefix strings
    public static final String RESOURCE_PREFIX_CLASSPATH = "classpath:";
    public static final String RESOURCE_PREFIX_FILE = "file:";
}

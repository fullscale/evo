package co.fs.evo.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static co.fs.evo.Constants.*;

public class ConfigService {

    private static final XLogger logger = XLoggerFactory.getXLogger(ConfigService.class);

    // our settings
    private Settings evoSettings;
    private Settings nodeSettings;

    private static ConfigService configService;

    /**
     * Get's an instance of the ConfigService. Using this method ensure's that only one ConfigService is created.
     * 
     * @return the config service.
     */
    public static ConfigService getConfigService() {
        if (configService == null) {
            configService = new ConfigService();
        }

        return configService;
    }

    /**
     * Constructor Initialize all settings and mappings
     */
    public ConfigService() {
        logger.entry();
        // setup sigar
        String sigarDir = "lib/sigar";
        logger.debug("sigar dir: {}", sigarDir);
        System.setProperty("org.hyperic.sigar.path", sigarDir);

        // java.util.logging -> slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // configure hazelcast to use slf4j
        System.setProperty("hazelcast.logging.type", "slf4j");
        
        // configure velocity logging
        Velocity.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        
        evoSettings = createEvoSettings();
        nodeSettings = createNodeSettings();
        logger.exit();
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a string value of the setting
     */
    public String get(String key) {
        return evoSettings.get(key);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a string value of the setting
     */
    public String get(String key, String def) {
        return evoSettings.get(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return an integer value of the setting
     */
    public int getInt(String key) {
        return evoSettings.getAsInt(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a integer value of the setting
     */
    public int getInt(String key, int def) {
        return evoSettings.getAsInt(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a float value of the setting
     */
    public float getFloat(String key) {
        return evoSettings.getAsFloat(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a float value of the setting
     */
    public float getFloat(String key, float def) {
        return evoSettings.getAsFloat(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a double value of the setting
     */
    public double getDouble(String key) {
        return evoSettings.getAsDouble(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a double value of the setting
     */
    public double getDouble(String key, double def) {
        return evoSettings.getAsDouble(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a long value of the setting
     */
    public long getLong(String key) {
        return evoSettings.getAsLong(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a long value of the setting
     */
    public long getLong(String key, long def) {
        return evoSettings.getAsLong(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a boolean value of the setting
     */
    public boolean getBool(String key) {
        return evoSettings.getAsBoolean(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a boolean value of the setting
     */
    public boolean getBool(String key, boolean def) {
        return evoSettings.getAsBoolean(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return an array value of the setting
     */
    public String[] getArray(String key) {
        return evoSettings.getAsArray(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a array value of the setting
     */
    public String[] getArray(String key, String[] def) {
        return evoSettings.getAsArray(key, def);
    }

    /**
     * Gets the internal evo settings
     * 
     * @return the evo settings object
     */
    public Settings getEvoSettings() {
        return evoSettings;
    }

    /**
     * Get the node settings
     * 
     * @return the node settings
     */
    public Settings getNodeSettings() {
        return nodeSettings;
    }

    /**
     * Gets the json html mapping as a string
     * 
     * @return html mapping
     */
    public String getHtmlMapping() {
        return getResourceContent("classpath:mappings/html.json");
    }

    /**
     * Gets the json css mapping as a string
     * 
     * @return css mapping
     */
    public String getCssMapping() {
        return getResourceContent("classpath:mappings/css.json");
    }

    /**
     * Gets the json js mapping as a string
     * 
     * @return js mapping
     */
    public String getJsMapping() {
        return getResourceContent("classpath:mappings/js.json");
    }
    
    /**
     * Gets the rendered velocity js template
     * 
     * @return the velocity template
     */
    public String getAngularTemplate(String app, String template) {
        logger.entry(app);
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/" + template + ".vm");

        Velocity.evaluate(context, rendered, template, tmpl);

        logger.exit();
        return rendered.toString();
    }

    /**
     * Gets the project resource as a string
     * 
     * @return contents of file/resource as a String
     */
    public String getAngularResource(String path) {
        return getResourceContent("file:resources/" + path);
    }
    
    /**
     * Returns the image as a base64 encoded String
     * 
     * @param resource the resource to get the contents for
     * @return the content of the resource as a string
     */
    public String getBase64Image(String resource) {
        logger.entry(resource);
        String content = null;
        InputStream in = null;

        try {
            in = getResourceInputStream("file:resources/" + resource);
            content = new String(Base64.encodeBase64(IOUtils.toByteArray(in)));
        } catch (Exception e) {
            logger.warn("Unable to load resource: {}", resource);
            logger.debug("exception", e);
        } finally {
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }

        logger.exit();
        return content;
    }
    
    /**
     * Gets the json images mapping as a string
     * 
     * @return images mapping
     */
    public String getImagesMapping() {
        return getResourceContent("classpath:mappings/images.json");
    }

    /**
     * Gets the json server mapping as a string
     * 
     * @return server mapping
     */
    public String getServerSideMapping() {
        return getResourceContent("classpath:mappings/server-side.json");
    }
    
    /**
     * Gets the json partials mapping as a string
     * 
     * @return partials mapping
     */
    public String getPartialsMapping() {
        return getResourceContent("classpath:mappings/partials.json");
    }
    
    /**
     * Gets the json lib mapping as a string
     * 
     * @return lib mapping
     */
    public String getLibMapping() {
        return getResourceContent("classpath:mappings/lib.json");
    }

    /**
     * Generates the main Evo settings. Settings are read from system properties or a user specified file. Some settings use
     * defaults when not set by the user.
     * 
     * @return the settings file
     */
    public Settings createEvoSettings() {
        logger.entry();
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        // get the default settings
        settings.put(getSettingsFromResource("classpath:evo.yml"));

        // do a build with the current settings so we can use the built-in getters not
        // available on the builder
        Settings defaults = settings.build();

        // http port
        int httpPort = Integer.parseInt(System.getProperty(PROPERTY_HTTP_PORT, defaults.get(SETTING_HTTP_PORT, "2600")));
        logger.debug("http port: {}", httpPort);
        settings.put(SETTING_HTTP_PORT, httpPort);

        // max threads
        int maxThreads = defaults.getAsInt(SETTING_HTTP_MAXTHREADS, 10);
        logger.debug("max threads: {}", maxThreads);
        settings.put(SETTING_HTTP_MAXTHREADS, maxThreads);

        // session timeout (in seconds)
        int sessionTimeout = defaults.getAsInt(SETTING_HTTP_SESSION_TIMEOUT, -1); // 12-hours
        logger.debug("session timeout in seconds: {}", sessionTimeout);
        settings.put(SETTING_HTTP_SESSION_TIMEOUT, sessionTimeout);

        // request logging enabled
        boolean requestLogEnabled = defaults.getAsBoolean(SETTING_HTTP_REQUESTLOG_ENABLED, false);
        logger.debug("request log enabled: {}", requestLogEnabled);
        settings.put(SETTING_HTTP_REQUESTLOG_ENABLED, requestLogEnabled);

        // https port
        int httpsPort = Integer.parseInt(System.getProperty(PROPERTY_HTTPS_PORT, defaults.get(SETTING_HTTPS_PORT, "2643")));
        logger.debug("https port: {}", httpsPort);
        settings.put(SETTING_HTTPS_PORT, httpsPort);

        // https enabled
        boolean httpsEnabled = Boolean.parseBoolean(System.getProperty(PROPERTY_HTTPS_ENABLED,
                defaults.get(SETTING_HTTPS_ENABLED, "false")));
        logger.debug("https enabled: {}", httpsEnabled);
        settings.put(SETTING_HTTPS_ENABLED, httpsEnabled);

        // https keypass
        String httpsKeypass = System.getProperty(PROPERTY_HTTPS_KEYPASS,
                defaults.get(SETTING_HTTPS_KEYPASS, "3f038de0-6606-11e1-b86c-0800200c9a66"));
        logger.debug("https keypass: {}", httpsKeypass);
        settings.put(SETTING_HTTPS_KEYPASS, httpsKeypass);

        // https keystore
        String httpsKeystore = System.getProperty(PROPERTY_HTTPS_KEYSTORE,
                defaults.get(SETTING_HTTPS_KEYSTORE, getHome() + "/etc/security/evo.default.keystore"));
        logger.debug("https keystore: {}", httpsKeystore);
        settings.put(SETTING_HTTPS_KEYSTORE, httpsKeystore);
        
        // spdy enabled
        boolean spdyEnabled = Boolean.parseBoolean(System.getProperty(PROPERTY_SPDY_ENABLED,
                defaults.get(SETTING_SPDY_ENABLED, "true")));
        logger.debug("spdy enabled: {}", spdyEnabled);
        settings.put(SETTING_SPDY_ENABLED, spdyEnabled);

        // hazelcast enabled
        boolean hazelcastEnabled = Boolean.parseBoolean(System.getProperty(PROPERTY_HAZELCAST_ENABLED,
                defaults.get(SETTING_HAZELCAST_ENABLED, "true")));
        logger.debug("hazelcast enabled: {}", hazelcastEnabled);
        settings.put(SETTING_HAZELCAST_ENABLED, hazelcastEnabled);

        // resource cache enabled
        boolean resourceCacheEnabled = Boolean.parseBoolean(System.getProperty(PROPERTY_CACHE_RESOURCES,
                defaults.get(SETTING_CACHE_RESOURCES_ENABLED, "true")));
        logger.debug("resource cache enabled: {}", resourceCacheEnabled);
        settings.put(SETTING_CACHE_RESOURCES_ENABLED, resourceCacheEnabled);

        // session cache enabled
        boolean sessionCacheEnabled = Boolean.parseBoolean(System.getProperty(PROPERTY_CACHE_SESSIONS,
                defaults.get(SETTING_CACHE_SESSIONS_ENABLED, "true")));
        logger.debug("session cache enabled: {}", sessionCacheEnabled);
        settings.put(SETTING_CACHE_SESSIONS_ENABLED, sessionCacheEnabled);

        // gzip compression enabled
        boolean gzipEnabled = Boolean.parseBoolean(System.getProperty(PROPERTY_GZIP_ENABLED,
                defaults.get(SETTING_GZIP_ENABLED, "true")));
        logger.debug("gzip compression enabled: {}", gzipEnabled);
        settings.put(SETTING_GZIP_ENABLED, gzipEnabled);
        
        // resource core threadpool size
        int corePoolSize = Integer.parseInt(System.getProperty(PROPERTY_COREPOOL_SIZE,
                defaults.get(SETTING_COREPOOL_SIZE, "10")));
        logger.debug("setting threadpool core size: {}", corePoolSize);
        settings.put(SETTING_COREPOOL_SIZE, corePoolSize);
        
        // resource max threadpool size
        int maxPoolSize = Integer.parseInt(System.getProperty(PROPERTY_MAXPOOL_SIZE,
                defaults.get(SETTING_MAXPOOL_SIZE, "100")));
        logger.debug("setting threadpool max size: {}", maxPoolSize);
        settings.put(SETTING_MAXPOOL_SIZE, maxPoolSize);
        
        // resource threadpool queue capacity
        int queueCapacity = Integer.parseInt(System.getProperty(PROPERTY_QUEUE_CAPACITY,
                defaults.get(SETTING_QUEUE_CAPACITY, "1000")));
        logger.debug("setting threadpool queue capacity: {}", queueCapacity);
        settings.put(SETTING_QUEUE_CAPACITY, queueCapacity);
        
        // resource threadpool keepalive seconds
        int keepaliveSeconds = Integer.parseInt(System.getProperty(PROPERTY_KEEPALIVE_SECONDS,
                defaults.get(SETTING_KEEPALIVE_SECONDS, "1")));
        logger.debug("setting threadpool keepalive seconds: {}", keepaliveSeconds);
        settings.put(SETTING_KEEPALIVE_SECONDS, keepaliveSeconds);
        
        // node name
        // set node name to the current hostname if the user does not specify one
        // order is system properties, settings file, hostname, "evo"
        String nodeName = System.getProperty(PROPERTY_NODE_NAME, defaults.get(SETTING_NODE_NAME));
        if (nodeName == null) {
            logger.debug("no node name specified, using hostname");
            try {
                nodeName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                logger.debug("unable to get hostname", e);
                nodeName = "evo";
            }
        }

        logger.debug("node name: {}", nodeName);
        settings.put(SETTING_NODE_NAME, nodeName);

        // cluster name
        // set to random string if not specified
        // order is system properties, settings file, random string
        String clusterName = System.getProperty(PROPERTY_CLUSTER_NAME, defaults.get(SETTING_CLUSTER_NAME));
        if (clusterName == null) {
            logger.debug("no cluster name specified, using random string");
            clusterName = UUID.randomUUID().toString();
        }

        logger.debug("cluster name: {}", clusterName);
        settings.put(SETTING_CLUSTER_NAME, clusterName);

        // network settings
        // default = multicast enabled, unicast disabled
        // when unicast.hosts is set in the settings file
        // multicast is disabled, unicast is enabled and we use the hostnames specified
        String[] unicastHosts = defaults.getAsArray(SETTING_UNICAST_HOSTS, null);

        logger.debug("unicastHosts: {}", (Object[])unicastHosts);
        if (unicastHosts != null) {
            logger.debug("unicast settings found");
            logger.debug("unicast enabled, multicast disabled");
            settings.put(SETTING_UNICAST_ENABLED, true);
            settings.put(SETTING_MULTICAST_ENABLED, false);
        } else {
            logger.debug("no unicast settings found");
            logger.debug("multicast enabled, unicast disabled");
            settings.put(SETTING_UNICAST_ENABLED, false);
            settings.put(SETTING_MULTICAST_ENABLED, true);
        }

        Settings finalSettings = settings.build();
        logger.exit(finalSettings.getAsMap());
        return finalSettings;
    }

    /**
     * Create the node settings from default settings and the main evo settings file.
     * 
     * @return the node settings.
     */
    public Settings createNodeSettings() {
        logger.entry();
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        // get the default node settings
        settings.put(getSettingsFromResource("classpath:defaultNodeSettings.yml"));

        // node name
        settings.put(SETTING_NODE_NAME, getNodeName());

        // cluster name
        settings.put(SETTING_CLUSTER_NAME, getClusterName());

        // use unicast vs. multicast
        boolean unicastEnabled = getUnicastEnabled();
        logger.debug("unicastEnabled: {}", unicastEnabled);
        if (unicastEnabled) {
            logger.debug("multicast disabled, unicast enabled");
            settings.put("discovery.zen.ping.multicast.enabled", false);
            settings.putArray("discovery.zen.ping.unicast.hosts", getUnicastHosts());
        }

        Settings finalNodeSettings = settings.build();
        logger.exit(finalNodeSettings.getAsMap());
        return finalNodeSettings;
    }

    /**
     * Get settings from a resource
     * 
     * @param resource the resource to get the settings from
     * @return the settings
     */
    public Settings getSettingsFromResource(String resource) {
        logger.entry(resource);
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        InputStream in = null;

        try {
            in = getResourceInputStream(resource);
            settings.loadFromStream(resource, in);
        } catch (Exception e) {
            logger.debug("Error loading settings", e);
        } finally {
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }

        logger.exit();
        return settings.build();
    }

    /**
     * Gets the content of a resource
     * 
     * @param resource the resource to get the contents for
     * @return the content of the resource as a string
     */
    public String getResourceContent(String resource) {
        logger.entry(resource);
        String content = null;
        InputStream in = null;

        try {
            in = getResourceInputStream(resource);
            content = IOUtils.toString(in, "UTF-8");
        } catch (Exception e) {
            logger.warn("Unable to load resource: {}", resource);
            logger.debug("exception", e);
        } finally {
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }

        logger.exit();
        return content;
    }

    /**
     * Gets a resource InputStream object
     * 
     * @param resource the resource to get
     * @return the resource input stream
     * @throws IOException
     */
    public InputStream getResourceInputStream(String resource) throws IOException {
        logger.entry(resource);

        ResourceLoader resourceLoader = null;

        if (resource.startsWith(RESOURCE_PREFIX_CLASSPATH)) {
            logger.debug("class path resource found");
            resourceLoader = new DefaultResourceLoader();
        } else {
            logger.debug("file system resource");
            resourceLoader = new FileSystemResourceLoader();
        }

        Resource r = resourceLoader.getResource(resource);
        logger.debug("{} exists: {}", resource, r.exists());

        logger.exit();
        return new BufferedInputStream(r.getInputStream());
    }

    public String getHome() {
        return System.getProperty(PROPERTY_HOME_DIR, System.getProperty("user.dir", "."));
    }

    public int getHttpPort() {
        return evoSettings.getAsInt(SETTING_HTTP_PORT, 2600);
    }

    public int getHttpMaxThreads() {
        return evoSettings.getAsInt(SETTING_HTTP_MAXTHREADS, 10);
    }

    public int getHttpSessionTimeout() {
        return evoSettings.getAsInt(SETTING_HTTP_SESSION_TIMEOUT, 43200);
    }

    public boolean getHttpRequestLogEnabled() {
        return evoSettings.getAsBoolean(SETTING_HTTP_REQUESTLOG_ENABLED, false);
    }

    public int getHttpsPort() {
        return evoSettings.getAsInt(SETTING_HTTPS_PORT, 2643);
    }

    public boolean getHttpsEnabled() {
        return evoSettings.getAsBoolean(SETTING_HTTPS_ENABLED, false);
    }

    public String getHttpsKeypass() {
        return evoSettings.get(SETTING_HTTPS_KEYPASS);
    }

    public String getHttpsKeystore() {
        return evoSettings.get(SETTING_HTTPS_KEYSTORE);
    }

    public boolean getSpdyEnabled() {
        return evoSettings.getAsBoolean(SETTING_SPDY_ENABLED, true);
    }
    
    public String getNodeName() {
        return evoSettings.get(SETTING_NODE_NAME);
    }

    public String getClusterName() {
        return evoSettings.get(SETTING_CLUSTER_NAME);
    }

    public boolean getMulticastEnabled() {
        return evoSettings.getAsBoolean(SETTING_MULTICAST_ENABLED, true);
    }

    public boolean getUnicastEnabled() {
        return evoSettings.getAsBoolean(SETTING_UNICAST_ENABLED, false);
    }

    public String[] getUnicastHosts() {
        return evoSettings.getAsArray(SETTING_UNICAST_HOSTS);
    }

    public boolean getHazelcastEnabled() {
        return evoSettings.getAsBoolean(SETTING_HAZELCAST_ENABLED, true);
    }

    public boolean getResourceCacheEnabled() {
        return evoSettings.getAsBoolean(SETTING_CACHE_RESOURCES_ENABLED, true);
    }

    public boolean getSessionCacheEnabled() {
        return evoSettings.getAsBoolean(SETTING_CACHE_SESSIONS_ENABLED, true);
    }
    
    public boolean getGzipEnabled() {
    	return evoSettings.getAsBoolean(SETTING_GZIP_ENABLED, true);
    }
    
    public int getCorePoolSize() {
    	return evoSettings.getAsInt(SETTING_COREPOOL_SIZE, 10);
    }
    
    public int getMaxPoolSize() {
    	return evoSettings.getAsInt(SETTING_MAXPOOL_SIZE, 100);
    }
    
    public int getQueueCapacity() {
    	return evoSettings.getAsInt(SETTING_QUEUE_CAPACITY, 1000);
    }
    
    public int getKeepaliveSeconds() {
    	return evoSettings.getAsInt(SETTING_KEEPALIVE_SECONDS, 1);
    }
}
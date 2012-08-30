package co.diji.cloud9.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

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

public class ConfigService {

    private static final XLogger logger = XLoggerFactory.getXLogger(ConfigService.class);

    // system property keys
    public static final String PROPERTY_C9_HOME = "c9.home";
    public static final String PROPERTY_HTTP_PORT = "c9.http.port";
    public static final String PROPERTY_HTTPS_PORT = "c9.https.port";
    public static final String PROPERTY_HTTPS_ENABLED = "c9.https.enabled";
    public static final String PROPERTY_HTTPS_KEYPASS = "c9.https.keypass";
    public static final String PROPERTY_HTTPS_KEYSTORE = "c9.https.keystore";
    public static final String PROPERTY_NODE_NAME = "c9.node.name";
    public static final String PROPERTY_CLUSTER_NAME = "c9.cluster.name";

    // settings keys
    public static final String SETTING_HOME_DIR = "home";
    public static final String SETTING_NODE_NAME = "node.name";
    public static final String SETTING_CLUSTER_NAME = "cluster.name";
    public static final String SETTING_HTTP_PORT = "network.http.port";
    public static final String SETTING_HTTPS_PORT = "network.https.port";
    public static final String SETTING_HTTPS_ENABLED = "network.https.enabled";
    public static final String SETTING_HTTPS_KEYPASS = "network.https.keypass";
    public static final String SETTING_HTTPS_KEYSTORE = "network.https.keystore";
    public static final String SETTING_UNICAST_HOSTS = "network.unicast.hosts";
    public static final String SETTING_UNICAST_ENABLED = "network.unicast.enabled";
    public static final String SETTING_MULTICAST_ENABLED = "network.multicast.enabled";

    // resource prefix strings
    public static final String RESOURCE_PREFIX_CLASSPATH = "classpath:";
    public static final String RESOURCE_PREFIX_FILE = "file:";

    // our settings
    private Settings cloud9Settings;
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

        cloud9Settings = createCloud9Settings();
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
        return cloud9Settings.get(key);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a string value of the setting
     */
    public String get(String key, String def) {
        return cloud9Settings.get(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return an integer value of the setting
     */
    public int getInt(String key) {
        return cloud9Settings.getAsInt(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a integer value of the setting
     */
    public int getInt(String key, int def) {
        return cloud9Settings.getAsInt(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a float value of the setting
     */
    public float getFloat(String key) {
        return cloud9Settings.getAsFloat(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a float value of the setting
     */
    public float getFloat(String key, float def) {
        return cloud9Settings.getAsFloat(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a double value of the setting
     */
    public double getDouble(String key) {
        return cloud9Settings.getAsDouble(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a double value of the setting
     */
    public double getDouble(String key, double def) {
        return cloud9Settings.getAsDouble(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a long value of the setting
     */
    public long getLong(String key) {
        return cloud9Settings.getAsLong(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a long value of the setting
     */
    public long getLong(String key, long def) {
        return cloud9Settings.getAsLong(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return a boolean value of the setting
     */
    public boolean getBool(String key) {
        return cloud9Settings.getAsBoolean(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a boolean value of the setting
     */
    public boolean getBool(String key, boolean def) {
        return cloud9Settings.getAsBoolean(key, def);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @return an array value of the setting
     */
    public String[] getArray(String key) {
        return cloud9Settings.getAsArray(key, null);
    }

    /**
     * Get a setting
     * 
     * @param key the setting to get
     * @param def the default value if not found
     * @return a array value of the setting
     */
    public String[] getArray(String key, String[] def) {
        return cloud9Settings.getAsArray(key, def);
    }

    /**
     * Gets the internal cloud9 settings
     * 
     * @return the cloud9 settings object
     */
    public Settings getCloud9Settings() {
        return cloud9Settings;
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
     * Gets the rendered velocity html template
     * 
     * @return the velocity template
     */
    public String getHtmlTemplate(String app) {
        logger.entry(app);
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/html.vm");

        Velocity.evaluate(context, rendered, "html", tmpl);
        logger.exit();
        return rendered.toString();
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
     * Gets the rendered velocity css template
     * 
     * @return the velocity template
     */
    public String getCssTemplate(String app) {
        logger.entry(app);
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/css.vm");

        Velocity.evaluate(context, rendered, "css", tmpl);
        logger.exit();
        return rendered.toString();
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
    public String getJsTemplate(String app) {
        logger.entry(app);
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/js.vm");

        Velocity.evaluate(context, rendered, "js", tmpl);
        logger.exit();
        return rendered.toString();
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
     * Gets the json controller mapping as a string
     * 
     * @return controller mapping
     */
    public String getControllerMapping() {
        return getResourceContent("classpath:mappings/controller.json");
    }

    /**
     * Gets the rendered velocity controller template
     * 
     * @return the velocity template
     */
    public String getControllerTemplate(String app) {
        logger.entry(app);
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/controller.vm");

        Velocity.evaluate(context, rendered, "controller", tmpl);
        logger.exit();
        return rendered.toString();
    }

    /**
     * Generates the main Cloud9 settings. Settings are read from system properties or a user specified file. Some settings use
     * defaults when not set by the user.
     * 
     * @return the settings file
     */
    public Settings createCloud9Settings() {
        logger.entry();
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        // get the default settings
        settings.put(getSettingsFromResource("classpath:cloud9.yml"));

        // do a build with the current settings so we can use the built-in getters not
        // available on the builder
        Settings defaults = settings.build();

        // http port
        int httpPort = Integer.parseInt(System.getProperty(PROPERTY_HTTP_PORT, defaults.get(SETTING_HTTP_PORT, "2600")));
        logger.debug("http port: {}", httpPort);
        settings.put(SETTING_HTTP_PORT, httpPort);

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
                defaults.get(SETTING_HTTPS_KEYSTORE, getHome() + "/etc/security/c9.default.keystore"));
        logger.debug("https keystore: {}", httpsKeystore);
        settings.put(SETTING_HTTPS_KEYSTORE, httpsKeystore);

        // node name
        // set node name to the current hostname if the user does not specify one
        // order is system properties, settings file, hostname, "cloud9"
        String nodeName = System.getProperty(PROPERTY_NODE_NAME, defaults.get(SETTING_NODE_NAME));
        if (nodeName == null) {
            logger.debug("no node name specified, using hostname");
            try {
                nodeName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                logger.debug("unable to get hostname", e);
                nodeName = "cloud9";
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

        logger.debug("unicastHosts: {}", unicastHosts);
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
     * Create the node settings from default settings and the main cloud9 settings file.
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
        return System.getProperty(PROPERTY_C9_HOME, System.getProperty("user.dir", "."));
    }

    public int getHttpPort() {
        return cloud9Settings.getAsInt(SETTING_HTTP_PORT, 2600);
    }

    public int getHttpsPort() {
        return cloud9Settings.getAsInt(SETTING_HTTPS_PORT, 2643);
    }

    public boolean getHttpsEnabled() {
        return cloud9Settings.getAsBoolean(SETTING_HTTPS_ENABLED, false);
    }

    public String getHttpsKeypass() {
        return cloud9Settings.get(SETTING_HTTPS_KEYPASS);
    }

    public String getHttpsKeystore() {
        return cloud9Settings.get(SETTING_HTTPS_KEYSTORE);
    }

    public String getNodeName() {
        return cloud9Settings.get(SETTING_NODE_NAME);
    }

    public String getClusterName() {
        return cloud9Settings.get(SETTING_CLUSTER_NAME);
    }

    public boolean getMulticastEnabled() {
        return cloud9Settings.getAsBoolean(SETTING_MULTICAST_ENABLED, true);
    }

    public boolean getUnicastEnabled() {
        return cloud9Settings.getAsBoolean(SETTING_UNICAST_ENABLED, false);
    }

    public String[] getUnicastHosts() {
        return cloud9Settings.getAsArray(SETTING_UNICAST_HOSTS);
    }
}

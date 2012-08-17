package co.diji.cloud9.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private Settings cloud9Settings;
    private Settings nodeSettings;

    @Autowired
    private WebApplicationContext applicationContext;

    /**
     * Initialize all settings and mappings
     */
    @PostConstruct
    public void init() {
        // setup sigar
        String sigarDir = applicationContext.getServletContext().getRealPath("/") + "/WEB-INF/lib/sigar";
        logger.debug("sigar dir: {}", sigarDir);
        System.setProperty("org.hyperic.sigar.path", sigarDir);
        
        // java.util.logging -> slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        
        // configure hazelcast to use slf4j
        System.setProperty("hazelcast.logging.type", "slf4j");
        
        cloud9Settings = getSettingsFromResource("classpath:cloud9.yml");
        nodeSettings = createNodeSettings();
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
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/html.vm");

        Velocity.evaluate(context, rendered, "html", tmpl);
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
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/css.vm");

        Velocity.evaluate(context, rendered, "css", tmpl);
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
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/js.vm");

        Velocity.evaluate(context, rendered, "js", tmpl);
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
        VelocityContext context = new VelocityContext();
        context.put("app", app);

        StringWriter rendered = new StringWriter();
        String tmpl = getResourceContent("classpath:templates/controller.vm");

        Velocity.evaluate(context, rendered, "controller", tmpl);
        return rendered.toString();
    }

    /**
     * Create the node settings from default settings, user settings, and system properties.
     * 
     * @return the node settings.
     */
    public Settings createNodeSettings() {
        logger.trace("in createNodeSettings");
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        // get the default node settings
        settings.put(getSettingsFromResource("classpath:defaultNodeSettings.yml"));

        // user specified node settings
        String userNodeSettingsFile = System.getProperty("c9.node.settings", null);
        logger.debug("userNodeSettingsFile: {}", userNodeSettingsFile);
        if (userNodeSettingsFile != null) {
            settings.put(getSettingsFromResource("file:" + userNodeSettingsFile));
        }

        // user specified node settings from system properties
        // set node name to the current hostname if the user does not specify one
        try {
            settings.put("node.name", System.getProperty("c9.node.name", InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException e) {
            logger.debug("unable to get hostname", e);
        }

        // set the cluster name to a random string if the user does not specify one
        settings.put("cluster.name", System.getProperty("c9.cluster.name", UUID.randomUUID().toString()));

        // use unicast vs. multicast
        String unicastHosts = System.getProperty("c9.unicast.hosts", null);
        logger.debug("c9.unicast.hosts: {}", unicastHosts);
        if (unicastHosts != null) {
            settings.put("discovery.zen.ping.multicast.enabled", false);
            settings.putArray("discovery.zen.ping.unicast.hosts", unicastHosts.split(","));
        }

        logger.debug("exit createNodeSettings: {}", settings.internalMap());
        return settings.build();
    }

    /**
     * Get settings from a resource
     * 
     * @param resource the resource to get the settings from
     * @return the settings
     */
    public Settings getSettingsFromResource(String resource) {
        logger.trace("in getSettingsForResource resource:{}", resource);
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        Resource settingsResource = applicationContext.getResource(resource);
        logger.debug("{} exists: {}", resource, settingsResource.exists());
        if (settingsResource.exists()) {
            try {
                InputStream is = settingsResource.getInputStream();
                settings.loadFromStream(settingsResource.getFilename(), is);
                logger.debug("settings read from {}: {}", resource, settings.internalMap());
                is.close();
            } catch (IOException e) {
                logger.debug("error reading settings from {}", resource, e);
            }
        }

        logger.trace("exit getSettingsFromResource: {}", settings.internalMap());
        return settings.build();
    }

    /**
     * Gets the content of a resource
     * 
     * @param resource the resource to get the contents for
     * @return the content of the resource as a string
     */
    public String getResourceContent(String resource) {
        logger.trace("in getResourceContent resource:{}", resource);
        String content = null;
        Resource res = applicationContext.getResource(resource);
        logger.debug("{} exists: {}", resource, res.exists());
        if (res.exists()) {
            try {
                InputStream is = res.getInputStream();
                content = IOUtils.toString(is, "UTF-8");
                is.close();
            } catch (IOException e) {
                logger.debug("error reading content of resource {}", resource);
            }
        }

        logger.trace("exit getResourceContent: {}", content);
        return content;
    }

    /**
     * Gets a resource File object
     * 
     * @param resource the resource to get
     * @return the resource File
     * @throws IOException
     */
    public File getResourceFile(String resource) throws IOException {
        logger.trace("in getResourceFile resource:{}", resource);
        Resource res = applicationContext.getResource(resource);
        File outFile = null;
        logger.debug("exists: {}", res.exists());
        if (res.exists()) {
            outFile = res.getFile();
        } else {
            logger.debug("Resource does not exist: {}", resource);
            throw new IOException("Resource does not exist: " + resource);
        }

        logger.trace("exit getResourceFile: {}", outFile);
        return outFile;
    }
}

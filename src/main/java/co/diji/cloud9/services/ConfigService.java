package co.diji.cloud9.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private Settings cloud9Settings;
    private Settings nodeSettings;
    private String htmlMapping;
    private String cssMapping;
    private String jsMapping;
    private String imagesMapping;
    private String controllerMapping;

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

        cloud9Settings = getSettingsFromResource("classpath:cloud9.yml");
        nodeSettings = createNodeSettings();
        htmlMapping = getResourceContent("classpath:mappings/html.json");
        cssMapping = getResourceContent("classpath:mappings/css.json");
        jsMapping = getResourceContent("classpath:mappings/js.json");
        imagesMapping = getResourceContent("classpath:mappings/images.json");
        controllerMapping = getResourceContent("classpath:mappings/controller.json");
    }

    /**
     * Get a settings
     * 
     * @param key the setting to get
     * @return a string value of the setting
     */
    public String get(String key) {
        return cloud9Settings.get(key);
    }

    /**
     * Get a settings
     * 
     * @param key the setting to get
     * @return an integer value of the setting
     */
    public int getInt(String key) {
        return cloud9Settings.getAsInt(key, null);
    }

    /**
     * Get a settings
     * 
     * @param key the setting to get
     * @return a float value of the setting
     */
    public float getFloat(String key) {
        return cloud9Settings.getAsFloat(key, null);
    }

    /**
     * Get a settings
     * 
     * @param key the setting to get
     * @return a double value of the setting
     */
    public double getDouble(String key) {
        return cloud9Settings.getAsDouble(key, null);
    }

    /**
     * Get a settings
     * 
     * @param key the setting to get
     * @return a long value of the setting
     */
    public long getLong(String key) {
        return cloud9Settings.getAsLong(key, null);
    }

    /**
     * Get a settings
     * 
     * @param key the setting to get
     * @return a boolean value of the setting
     */
    public boolean getBool(String key) {
        return cloud9Settings.getAsBoolean(key, null);
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
        return htmlMapping;
    }

    /**
     * Gets the json css mapping as a string
     * 
     * @return css mapping
     */
    public String getCssMapping() {
        return cssMapping;
    }

    /**
     * Gets the json js mapping as a string
     * 
     * @return js mapping
     */
    public String getJsMapping() {
        return jsMapping;
    }

    /**
     * Gets the json images mapping as a string
     * 
     * @return images mapping
     */
    public String getImagesMapping() {
        return imagesMapping;
    }

    /**
     * Gets the json controller mapping as a string
     * 
     * @return controller mapping
     */
    public String getControllerMapping() {
        return controllerMapping;
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
}

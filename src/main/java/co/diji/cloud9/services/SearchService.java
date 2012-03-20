package co.diji.cloud9.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private Node node;
    private Client client;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void booststrap() {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        
        // cloud9 default settings
        Resource conf = applicationContext.getResource("classpath:cloud9.yml");
        logger.debug("cloud9.yml exists: {}", conf.exists());
        if (conf.exists()) {
            try {
                InputStream is = conf.getInputStream();
                settings.loadFromStream(conf.getFilename(), is);
                logger.debug("settings: {}", settings.internalMap());
                is.close();
            } catch (IOException e) {
                logger.debug("error reading settings", e);
            }
        }

        // custom user settings from file
        String userSettingsFile = System.getProperty("c9.settings", null);
        logger.debug("user settings file: {}", userSettingsFile);
        if (userSettingsFile != null) {
            Resource userConf = applicationContext.getResource("file:" + userSettingsFile);
            logger.debug("userConf exists: {}", userConf.exists());
            if (userConf.exists()) {
                try {
                    InputStream is = userConf.getInputStream();
                    settings.loadFromStream(userSettingsFile, is);
                    logger.debug("settings: {}", settings.internalMap());
                    is.close();
                } catch (IOException e) {
                    logger.warn("error loading user settings", e);
                } 
            }
        }

        // custom user settings from system properties
        // set node name to the current hostname
        try {
            settings.put("node.name", System.getProperty("c9.node.name", InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException e) {
            logger.debug("unable to get hostname", e);
        }
        
        // set the cluster name to a random string
        settings.put("cluster.name", System.getProperty("c9.cluster.name", UUID.randomUUID().toString()));
        
        // use unicast vs. multicast
        String unicastHosts = System.getProperty("c9.unicast.hosts", null);
        logger.debug("c9.unicast.hosts: {}", unicastHosts);
        if (unicastHosts != null) {
            settings.put("discovery.zen.ping.multicast.enabled", false);
            settings.putArray("discovery.zen.ping.unicast.hosts", unicastHosts.split(","));
        }
        
        logger.debug("final settings: {}", settings.internalMap());
        node = NodeBuilder.nodeBuilder().settings(settings).node();
        client = node.client();
    }

    @PreDestroy
    public void shutdown() {
        if (node != null) {
            node.close();
        }
    }

    public Node getNode() {
        return node;
    }

    public Client getClient() {
        return client;
    }

}

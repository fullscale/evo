package co.diji.cloud9.services;

import java.io.IOException;
import java.io.InputStream;

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
        Resource conf = applicationContext.getResource("classpath:cloud9.yml");
        logger.debug("cloud9.yml exists: {}", conf.exists());
        if (conf.exists()) {
            try {
                InputStream is = conf.getInputStream();
                settings.loadFromStream(conf.getFilename(), is);
                logger.debug("settings: {}", settings);
                is.close();
            } catch (IOException e) {
                logger.debug("error reading settings", e);
            }
        }

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

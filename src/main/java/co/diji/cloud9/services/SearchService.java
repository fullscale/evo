package co.diji.cloud9.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalNode;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private Node node;
    private Client client;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void booststrap() {
        logger.info("bootstraping service");
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        Resource conf = applicationContext.getResource("classpath:cloud9.yml");
        if (conf.exists()) {
            try {
                InputStream is = conf.getInputStream();
                settings.loadFromStream(conf.getFilename(), is);
                is.close();
            } catch (IOException e) {
                // no-op
            }
        }

        node = NodeBuilder.nodeBuilder().settings(settings).node();
        client = node.client();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("stop service");
        if (node != null) {
            node.close();
        }
    }

}

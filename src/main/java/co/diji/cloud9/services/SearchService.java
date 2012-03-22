package co.diji.cloud9.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private Node node;
    private Client client;

    @Autowired
    private WebApplicationContext applicationContext;

    /**
     * Initialize and start our ElasticSearch node.
     */
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

        // setup sigar
        String sigarDir = applicationContext.getServletContext().getRealPath("/") + "/WEB-INF/lib/sigar";
        logger.debug("sigar dir: {}", sigarDir);
        System.setProperty("org.hyperic.sigar.path", sigarDir);

        node = NodeBuilder.nodeBuilder().settings(settings).node();
        client = node.client();
    }

    /**
     * Cleanly shutdown our ElasticSearch node.
     */
    @PreDestroy
    public void shutdown() {
        if (node != null) {
            node.close();
        }
    }

    /**
     * Get the internal ElasticSearch Node object
     * 
     * @return the internal Node object
     */
    public Node getNode() {
        return node;
    }

    /**
     * Get the internal ElasticSearch Client object
     * 
     * @return the internal Client object
     */
    public Client getClient() {
        return client;
    }

    /**
     * Gets the cluster health
     * 
     * @return the cluster health, null when there is an error
     */
    public ClusterHealthResponse getClusterHealth() {
        logger.trace("in getClusterHealth");
        ClusterHealthResponse resp = null;
        ListenableActionFuture<ClusterHealthResponse> healthAction = client.admin().cluster().prepareHealth().execute();

        try {
            resp = healthAction.actionGet();
        } catch (ElasticSearchException e) {
            logger.debug("Error getting cluster health", e);
        }

        logger.trace("exit getClusterHealth: {}", resp);
        return resp;
    }

    /**
     * Gets the status of an index.
     * 
     * @param indices a list of index names to get the status for
     * @return a map where the key is the index name and the value is the status for that index, null on error
     */
    public Map<String, IndexStatus> getIndexStatus(String... indices) {
        logger.trace("in getIndexStatus indices:{}", indices);
        Map<String, IndexStatus> indexStatus = null;
        ListenableActionFuture<IndicesStatusResponse> action = client.admin().indices().prepareStatus(indices).execute();
        try {
            IndicesStatusResponse resp = action.actionGet();
            indexStatus = resp.indices();
        } catch (ElasticSearchException e) {
            logger.debug("Error getting index status", e);
        }

        logger.trace("exit getIndexStatus: {}", indexStatus);
        return indexStatus;
    }

    /**
     * Get information about nodes in the cluster.
     * 
     * @return a map where the key is the node id and the value is the info for that node, null on error
     */
    public Map<String, NodeInfo> nodeInfo() {
        logger.trace("nodeInfo");
        Map<String, NodeInfo> nodeInfo = null;
        ActionFuture<NodesInfoResponse> action = client.admin().cluster().nodesInfo(new NodesInfoRequest());

        try {
            NodesInfoResponse resp = action.actionGet();
            nodeInfo = new HashMap<String, NodeInfo>();
            for (NodeInfo info : resp.getNodes()) {
                nodeInfo.put(info.node().id(), info);
            }
        } catch (ElasticSearchException e) {
            logger.debug("Error getting node info", e);
        }

        logger.trace("nodeInfo: {}", nodeInfo);
        return nodeInfo;
    }

    /**
     * Get stats about nodes in the cluster.
     * 
     * @return a map where the key is the node id and the value is the info for that node, null on error
     */
    public Map<String, NodeStats> nodeStats() {
        logger.trace("nodeStats");
        Map<String, NodeStats> nodeStats = null;
        ActionFuture<NodesStatsResponse> action = client.admin().cluster().nodesStats(new NodesStatsRequest());

        try {
            NodesStatsResponse resp = action.actionGet();
            nodeStats = new HashMap<String, NodeStats>();
            for (NodeStats stats : resp.getNodes()) {
                nodeStats.put(stats.getNode().id(), stats);
            }
        } catch (ElasticSearchException e) {
            logger.debug("Error getting node stats", e);
        }

        logger.trace("nodeStats: {}", nodeStats);
        return nodeStats;
    }
}
package co.diji.cloud9.services;

import java.util.HashMap;
import java.util.Map;

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
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import org.elasticsearch.monitor.os.OsStats;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private Node node;
    private Client client;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ConfigService config;

    /**
     * Initialize and start our ElasticSearch node.
     */
    @PostConstruct
    public void booststrap() {
        node = NodeBuilder.nodeBuilder().settings(config.getNodeSettings()).node();
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
     * Gets the state of the cluster
     * 
     * @return the cluster state, null when there is an error
     */
    public ClusterState getClusterState() {
        logger.trace("in getClusterState");
        ClusterState clusterState = null;
        ListenableActionFuture<ClusterStateResponse> action = client.admin().cluster().prepareState().execute();

        try {
            ClusterStateResponse resp = action.actionGet();
            clusterState = resp.state();
        } catch (ElasticSearchException e) {
            logger.debug("Error getting cluster state");
        }

        logger.trace("exit getClusterState: {}", clusterState);
        return clusterState;
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
    public Map<String, NodeInfo> getNodeInfo() {
        logger.trace("in getNodeInfo");
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

        logger.trace("exit getNodeInfo: {}", nodeInfo);
        return nodeInfo;
    }

    /**
     * Get stats about nodes in the cluster.
     * 
     * @return a map where the key is the node id and the value is the info for that node, null on error
     */
    public Map<String, NodeStats> getNodeStats() {
        logger.trace("in getNodeStats");
        Map<String, NodeStats> nodeStats = null;
        ActionFuture<NodesStatsResponse> action = client.admin().cluster().nodesStats(new NodesStatsRequest());

        try {
            NodesStatsResponse resp = action.actionGet();
            nodeStats = new HashMap<String, NodeStats>();
            for (NodeStats stats : resp.getNodes()) {
                OsStats ostats = stats.getOs();
                System.out.println(ostats);
                //System.out.println(ostats.timestamp());
                nodeStats.put(stats.getNode().id(), stats);
            }
        } catch (ElasticSearchException e) {
            logger.debug("Error getting node stats", e);
        }

        logger.trace("exit getNodeStats: {}", nodeStats);
        return nodeStats;
    }
}
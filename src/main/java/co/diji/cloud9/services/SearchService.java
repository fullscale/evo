package co.diji.cloud9.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import co.diji.cloud9.exceptions.index.IndexCreationException;
import co.diji.cloud9.exceptions.index.IndexException;
import co.diji.cloud9.exceptions.index.IndexExistsException;
import co.diji.cloud9.utils.C9Helper;

@Service
public class SearchService {

    private static final String SYSTEM_INDEX = "sys";
    private static final String APP_SUFFIX = ".app";
    private static final String[] RESERVED_APPS = {"css.app", "js.app", "images.app"};

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
     * Gets all the collections and their status.
     * 
     * @param collections the collections to get the status for
     * @return a map where the key is the collection name and the value is the status
     */
    public Map<String, IndexStatus> getCollectionStatus(String... collections) {
        logger.trace("in getCollectionStatus collections:{}, collections");
        Map<String, IndexStatus> collectionStatus = new HashMap<String, IndexStatus>();
        Map<String, IndexStatus> indices = getIndexStatus(collections);

        logger.debug("indices: {}", indices);
        if (indices != null) {
            for (Entry<String, IndexStatus> index : indices.entrySet()) {
                String indexName = index.getKey();
                IndexStatus indexStatus = index.getValue();
                logger.debug("indexName: {}", indexName);
                if (!indexName.equals(SYSTEM_INDEX) && !indexName.endsWith(APP_SUFFIX)) {
                    collectionStatus.put(indexName, indexStatus);
                }
            }
        }

        logger.trace("exit getCollectionStatus: {}", collectionStatus);
        return collectionStatus;
    }

    /**
     * Gets all the apps and their status.
     * 
     * @param apps the apps to get the status for
     * @return a map where the key is the app name and the value is the status
     */
    public Map<String, IndexStatus> getAppStatus(String... apps) {
        logger.trace("in getAppStatus apps:{}", apps);
        String[] appsWithSuffix = new String[apps.length];
        for (int appIdx = 0; appIdx < apps.length; appIdx++) {
            String app = apps[appIdx];
            if (!app.endsWith(APP_SUFFIX)) {
                appsWithSuffix[appIdx] = app + APP_SUFFIX;
            } else {
                appsWithSuffix[appIdx] = app;
            }
        }

        Map<String, IndexStatus> appStatus = new HashMap<String, IndexStatus>();
        Map<String, IndexStatus> indices = getIndexStatus(appsWithSuffix);

        logger.debug("indices: {}", indices);
        if (indices != null) {
            for (Entry<String, IndexStatus> index : indices.entrySet()) {
                String indexName = index.getKey();
                IndexStatus indexStatus = index.getValue();
                logger.debug("indexName: {}", indexName);
                if (!indexName.equals(SYSTEM_INDEX) && indexName.endsWith(APP_SUFFIX)) {
                    appStatus.put(indexName.replace(APP_SUFFIX, ""), indexStatus);
                }
            }
        }

        logger.trace("exit getAppStatus: {}", appStatus);
        return appStatus;
    }

    /**
     * Gets the total number of collection documents that exist in the cluster
     * 
     * @return the sum of all collection document counts.
     */
    public long getTotalCollectionDocCount() {
        logger.trace("in getTotalCollectionDocCount");
        long numDocs = 0;
        Map<String, IndexStatus> collections = getCollectionStatus();

        logger.debug("collections: {}", collections);
        if (collections != null) {
            for (Entry<String, IndexStatus> collection : collections.entrySet()) {
                numDocs = numDocs + collection.getValue().docs().numDocs();
            }
        }

        logger.trace("exit getTotalCollectionDocCount: {}", numDocs);
        return numDocs;
    }

    /**
     * Gets the total number of app documents that exist in the cluster
     * 
     * @return the sum of all app document counts
     */
    public long getTotalAppDocCount() {
        logger.trace("in getTotalAppDocCount");
        long numDocs = 0;
        Map<String, IndexStatus> apps = getAppStatus();

        logger.debug("apps: {}", apps);
        if (apps != null) {
            for (Entry<String, IndexStatus> app : apps.entrySet()) {
                numDocs = numDocs + app.getValue().docs().numDocs();
            }
        }

        logger.trace("exit getTotalAppDocCount: {}", numDocs);
        return numDocs;
    }

    /**
     * Get information about nodes in the cluster.
     * 
     * @return a map where the key is the node id and the value is the info for that node, null on error
     */
    public Map<String, NodeInfo> getNodeInfo() {
        logger.trace("in getNodeInfo");
        Map<String, NodeInfo> nodeInfo = null;
        ActionFuture<NodesInfoResponse> action = client.admin().cluster().nodesInfo(new NodesInfoRequest().all());

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
        ActionFuture<NodesStatsResponse> action = client.admin().cluster().nodesStats(new NodesStatsRequest().all());

        try {
            NodesStatsResponse resp = action.actionGet();
            nodeStats = new HashMap<String, NodeStats>();
            for (NodeStats stats : resp.getNodes()) {
                nodeStats.put(stats.getNode().id(), stats);
            }
        } catch (ElasticSearchException e) {
            logger.debug("Error getting node stats", e);
        }

        logger.trace("exit getNodeStats: {}", nodeStats);
        return nodeStats;
    }

    /**
     * Checks if the specified index exists or not
     * 
     * @param name the name of the index to check for
     * @return true if exists, false otherwise
     */
    public boolean hasIndex(String name) {
        logger.trace("in hasIndex name:{}", name);
        boolean exists = false;
        ClusterState state = getClusterState();
        logger.debug("cluster state: {}", state);
        if (state != null) {
            exists = state.metaData().hasIndex(name);
        }

        logger.trace("exit hasIndex: {}", exists);
        return exists;
    }

    /**
     * Creates an index with default settings
     * 
     * @param name the name of the index to create
     * @return if the creation of the index was ack'd by the cluster
     * @throws IndexException
     */
    public boolean createIndex(String name) throws IndexException {
        logger.trace("in createIndex name:{}", name);
        return createIndex(name, null, null);
    }

    /**
     * Creates an index with the the given type mappings
     * 
     * @param name the name of the index to create
     * @param mappings the key is the type of the mapping, the value is the json mapping
     * @return if the creation of the index was ack'd by the cluster
     * @throws IndexException
     */
    public boolean createIndex(String name, Map<String, String> mappings) throws IndexException {
        logger.trace("in createIndex name:{} mappings:{}", name, mappings);
        return createIndex(name, null, mappings);
    }

    /**
     * Creates an index with the specified number of shards and replicas
     * 
     * @param name the name of the index to create
     * @param shards the number of shards for the index
     * @param replicas the number of replicas for the idnex
     * @return if the creation of the index was ack'd by the cluster
     * @throws IndexException
     */
    public boolean createIndex(String name, int shards, int replicas) throws IndexException {
        logger.trace("in createIndex name:{} shards:{} replicas:{}", new Object[]{name, shards, replicas});
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        settings.put("number_of_shards", shards);
        settings.put("number_of_replicas", replicas);
        return createIndex(name, settings.build(), null);
    }

    /**
     * Creates an index with the specified number of shards, replicas, and type mappings.
     * 
     * @param name the name of the index to create
     * @param shards the number of shards for the index
     * @param replicas the number of replicas for the index
     * @param mappings the key is the type of mapping, the value is the json mapping
     * @return if the creation of the index was ack'd by the cluster
     * @throws IndexException
     */
    public boolean createIndex(String name, int shards, int replicas, Map<String, String> mappings) throws IndexException {
        logger.trace("in createIndex name:{} shards:{} replicas:{} mappings:{}", new Object[]{name, shards, replicas, mappings});
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        settings.put("number_of_shards", shards);
        settings.put("number_of_replicas", replicas);
        return createIndex(name, settings.build(), mappings);
    }

    /**
     * Creates an index with the specified settings and mappings.
     * 
     * @param name the name of the index to create
     * @param settings the settings for the index
     * @param mappings the key is the type of mapping, the value is the json mapping
     * @return if the creation of the index was ack'd by the cluster
     * @throws IndexException
     */
    public boolean createIndex(String name, Settings settings, Map<String, String> mappings) throws IndexException {
        logger.trace("in createIndex name:{} settings:{} mappings:{}", new Object[]{name, settings, mappings});
        boolean valid = C9Helper.isValidName(name);
        logger.debug("is valid name: {}", valid);
        if (!valid) {
            throw new IndexCreationException("Invalid index name: " + name);
        }

        boolean exists = hasIndex(name);
        logger.debug("index exists: {}", exists);
        if (exists) {
            throw new IndexExistsException("Index already exists: " + name);
        }

        CreateIndexRequest request;
        logger.debug("settings: {}", settings);
        if (settings != null) {
            request = new CreateIndexRequest(name, settings);
        } else {
            request = new CreateIndexRequest(name);
        }

        logger.debug("mappings: {}", mappings);
        if (mappings != null) {
            for (Entry<String, String> mapping : mappings.entrySet()) {
                request.mapping(mapping.getKey(), mapping.getValue());
            }
        }

        ActionFuture<CreateIndexResponse> action = client.admin().indices().create(request);
        CreateIndexResponse resp = null;

        try {
            resp = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.debug("Error creating index: {}", name);
            throw new IndexCreationException("Error creating index: " + name, e);
        }

        logger.trace("exit createIndex: {}", resp.acknowledged());
        return resp.acknowledged();
    }

    /**
     * Create application index with default 1 shard and 1 replica
     * 
     * @param appName the name of the application
     * @return if the application was ack'd by the cluster or not
     * @throws IndexException
     */
    public boolean createAppIndex(String appName) throws IndexException {
        logger.trace("in createAppIndex appName:{}", appName);
        return createAppIndex(appName, 1, 1);
    }

    /**
     * Create application with the specified number of shards and replicas
     * 
     * @param appName the name of the application
     * @param shards the number of shards for the application
     * @param replicas the number of replicas for the application
     * @return if the application was ack'd by the cluster or not
     * @throws IndexException
     */
    public boolean createAppIndex(String appName, int shards, int replicas) throws IndexException {
        logger.trace("in createAppIndex appName:{} shards:{} replicas:{}", new Object[]{appName, shards, replicas});
        if (!appName.endsWith(APP_SUFFIX)) {
            appName = appName + APP_SUFFIX;
        }

        logger.debug("appName: {}", appName);
        if (Arrays.asList(RESERVED_APPS).contains(appName)) {
            throw new IndexCreationException("Invalid application name: " + appName);
        }

        Map<String, String> mappings = new HashMap<String, String>();
        mappings.put("html", config.getHtmlMapping());
        mappings.put("css", config.getCssMapping());
        mappings.put("js", config.getJsMapping());
        mappings.put("images", config.getImagesMapping());
        mappings.put("controllers", config.getControllerMapping());

        boolean ack = createIndex(appName, shards, replicas, mappings);
        logger.trace("exit createAppIndex: {}", ack);
        return ack;
    }
}
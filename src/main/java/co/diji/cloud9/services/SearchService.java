package co.diji.cloud9.services;

import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
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
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.delete.DeleteMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.exceptions.index.IndexCreationException;
import co.diji.cloud9.exceptions.index.IndexException;
import co.diji.cloud9.exceptions.index.IndexExistsException;
import co.diji.cloud9.exceptions.index.IndexMissingException;
import co.diji.cloud9.exceptions.mapping.MappingException;
import co.diji.cloud9.exceptions.type.TypeCreationException;
import co.diji.cloud9.exceptions.type.TypeExistsException;
import co.diji.cloud9.utils.C9Helper;

@Service
public class SearchService {

    private static final String SYSTEM_INDEX = "sys";
    private static final String APP_SUFFIX = ".app";
    private static final String[] RESERVED_APPS = {"css.app", "js.app", "images.app"};
    private static final String[] VALID_TYPES = {"conf", "html", "css", "images", "js", "controllers"};

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private Node node;
    private Client client;

    @Autowired
    private ConfigService config;

    /**
     * Utility method to make sure a list of apps has the app suffix
     * 
     * @param apps the list of apps to check
     * @return the list of apps, all containing the app suffix
     */
    public String[] appsWithSuffix(String... apps) {
        String[] appsWithSuffix = new String[apps.length];
        for (int appIdx = 0; appIdx < apps.length; appIdx++) {
            String app = apps[appIdx];
            if (!app.endsWith(APP_SUFFIX)) {
                appsWithSuffix[appIdx] = app + APP_SUFFIX;
            } else {
                appsWithSuffix[appIdx] = app;
            }
        }

        return appsWithSuffix;
    }

    /**
     * Initialize and start our ElasticSearch node.
     * 
     * @throws Cloud9Exception
     */
    @PostConstruct
    public void booststrap() throws Cloud9Exception {
        logger.trace("in bootstrap");
        logger.info("Initializing data/search services");
        node = NodeBuilder.nodeBuilder().settings(config.getNodeSettings()).node();
        client = node.client();

        logger.info("Waiting for cluster status");
        ClusterHealthResponse health = getClusterHealth(true);
        logger.info("Cluster Initialized [name:{}, status:{}]", health.clusterName(), health.status());

        setupSystemIndex();
        logger.info("Node is bootstrapped and online");
        logger.trace("exit bootstrap");
    }

    /**
     * Cleanly shutdown our ElasticSearch node.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutdown initiated");
        if (node != null) {
            logger.info("Node is terminating");
            node.close();
        }
        logger.info("Shutdown complete");
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
     * Creates the Cloud9 system index
     * 
     * @throws Cloud9Exception
     */
    public void setupSystemIndex() throws Cloud9Exception {
        logger.trace("in setupSystemIndex");
        boolean hasSysIndex = hasIndex(SYSTEM_INDEX);
        logger.debug("hasSysIndex: {}", hasSysIndex);
        if (!hasSysIndex) {
            logger.info("Initializing system accounts");
            try {
                createIndex(SYSTEM_INDEX, 1, 1);
            } catch (IndexException e) {
                logger.error("Error creating system index");
                throw new Cloud9Exception("Error creating system index", e);
            }

            Map<String, Object> admin = new HashMap<String, Object>();
            admin.put("name", "Administrator");
            admin.put("role", "admin");
            admin.put("uname", "admin");
            admin.put("email", "root@localhost");
            admin.put("password", config.get("admin.password"));

            logger.debug("admin user: {}", admin);
            indexDoc(SYSTEM_INDEX, "users", "admin", admin);
        } else {
            logger.info("Recovering system account information");
        }

        logger.trace("exit setupSystemIndex");
    }

    /**
     * Gets the cluster health
     * 
     * @return the cluster health, null when there is an error
     */
    public ClusterHealthResponse getClusterHealth() {
        logger.trace("in getClusterHealth");
        return getClusterHealth(false);
    }

    /**
     * Gets the cluster health
     * 
     * @param waitForYellow if we wait for the cluster to be initialized
     * @return the cluster health, null when there is an error
     */
    public ClusterHealthResponse getClusterHealth(boolean waitForYellow) {
        logger.trace("in getClusterHealth waitForYellow:{}", waitForYellow);
        ClusterHealthResponse resp = null;
        ClusterHealthRequestBuilder healthBuilder = client.admin().cluster().prepareHealth();

        logger.debug("wait for yellow: {}", waitForYellow);
        if (waitForYellow) {
            healthBuilder.setWaitForYellowStatus();
        }

        ListenableActionFuture<ClusterHealthResponse> healthAction = healthBuilder.execute();

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
        String[] appsWithSuffix = appsWithSuffix(apps);
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
     * Checks if the specified app exists or not
     * 
     * @param appName the name of the app to check for
     * @return true if exists, false otherwise
     */
    public boolean hasApp(String appName) {
        logger.trace("in hasApp appName:{}", appName);
        if (!appName.endsWith(APP_SUFFIX)) {
            appName = appName + APP_SUFFIX;
            logger.debug("final appName: {}", appName);
        }

        return hasIndex(appName);
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
                logger.debug("mapping key:{} value:{}", mapping.getKey(), mapping.getValue());
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
     * Create collection with default number of shards and replicas
     * 
     * @param name the collection name
     * @return if the collection was ack'd by the cluster or not
     * @throws IndexException
     */
    public boolean createCollectionIndex(String name) throws IndexException {
        logger.trace("in createCollectionIndex name:{}", name);

        if (name.equals(SYSTEM_INDEX) || name.endsWith(APP_SUFFIX)) {
            throw new IndexCreationException("Invliad collection name: " + name);
        }

        return createIndex(name);
    }

    /**
     * Create collection with the specified number of shards and replicas
     * 
     * @param name the collection name
     * @param shards the number of shards for the collection
     * @param replicas the number of replicas for the collection
     * @return if the collection was ack'd by the cluster or not
     * @throws IndexException
     */
    public boolean createCollectionIndex(String name, int shards, int replicas) throws IndexException {
        logger.trace("in createCollectionIndex name:{}, shards:{}, replicas:{}", new Object[]{name, shards, replicas});

        if (name.equals(SYSTEM_INDEX) || name.endsWith(APP_SUFFIX)) {
            throw new IndexCreationException("Invliad collection name: " + name);
        }

        return createIndex(name, shards, replicas);
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

    /**
     * Index a document
     * 
     * @param index what index to store the document in
     * @param type the type of the indexed document
     * @param id the document id
     * @param source the document contents
     * @return the index response object, null on error
     */
    public IndexResponse indexDoc(String index, String type, String id, Map<String, Object> source) {
        logger.trace("in indexDoc index:{}, type:{}, id:{}, source:{}", new Object[]{index, type, id, source});
        IndexRequest req = new IndexRequest(index, type, id).source(source);
        ActionFuture<IndexResponse> action = client.index(req);
        IndexResponse resp = null;
        try {
            resp = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.warn("Error indexing document: index:{}, type:{}, id:{}. source:{}", new Object[]{index, type, id, source}, e);
        }

        logger.trace("exit indexDoc: {}", resp);
        return resp;
    }

    /**
     * Indexes a document specifically for apps
     * 
     * @param app the app to index the doc in
     * @param type the resource type of the doc
     * @param id the id of the doc
     * @param code the source code
     * @param mime the mime type of the source code
     * @return the index response object, null on error
     */
    public IndexResponse indexAppDoc(String app, String type, String id, String code, String mime) {
        logger.trace("in indexAppDoc app:{}, type:{}, id:{}, code:{}, mime:{}", new Object[]{app, type, id, code, mime});
        if (!app.endsWith(APP_SUFFIX)) {
            logger.debug("final app name: {}", app);
            app = app + APP_SUFFIX;
        }

        Map<String, Object> source = new HashMap<String, Object>();
        source.put("code", code);
        source.put("mime", mime);
        logger.debug("app {} source: {}", app, source);

        return indexDoc(app, type, id, source);
    }

    /**
     * Deletes a document from an index
     * 
     * @param index the index you want to delete the document from
     * @param type the type you want to delete the document from
     * @param id the document id of the document you want to delete
     * @return the delete response object, null on error
     */
    public DeleteResponse deleteDoc(String index, String type, String id) {
        logger.trace("in deleteDoc index:{} type:{} id:{}", new Object[]{index, type, id});
        DeleteRequest req = new DeleteRequest(index, type, id);
        ActionFuture<DeleteResponse> action = client.delete(req);
        DeleteResponse resp = null;

        try {
            resp = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.warn("Error delete document: index:{}, type:{}, id:{}", new Object[]{index, type, id});
        }

        logger.trace("exit deleteDoc: {}", resp);
        return resp;
    }

    /**
     * Creates an app
     * 
     * @param appName the name of the app
     * @throws IndexException
     */
    public void createApp(String appName) throws IndexException {
        logger.trace("in createApp appName:{}", appName);
        appName = appName.replace(APP_SUFFIX, "");
        createAppIndex(appName);
        indexAppDoc(appName, "html", "index.html", config.getHtmlTemplate(appName), "text/html");
        indexAppDoc(appName, "css", "style.css", config.getCssTemplate(appName), "text/css");
        indexAppDoc(appName, "js", appName + ".js", config.getJsTemplate(appName), "application/javascript");
        indexAppDoc(appName, "controllers", "examples.js", config.getControllerTemplate(appName), "application/javascript");
        logger.trace("exit createApp");
    }

    /**
     * Refreshes the specified indices
     * 
     * @param indices the indices to refresh
     * @return the refresh response, null on error
     */
    public RefreshResponse refreshIndex(String... indices) {
        logger.trace("in refreshIndex indices:{}", indices);
        RefreshResponse refreshResponse = null;
        ListenableActionFuture<RefreshResponse> action = client.admin().indices().prepareRefresh(indices).execute();

        try {
            refreshResponse = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.debug("Error refreshing indices:{}", indices);
        }

        logger.trace("exit refreshIndex: {}", refreshResponse);
        return refreshResponse;
    }

    /**
     * Refreshes the specified apps
     * 
     * @param apps the apps to refresh
     * @return the refresh status, null on error
     */
    public RefreshResponse refreshApp(String... apps) {
        logger.trace("in refreshApp apps:{}", apps);
        String[] appsWithSuffix = appsWithSuffix(apps);
        return refreshIndex(appsWithSuffix);
    }

    /**
     * Deletes one or more index
     * 
     * @param indices the list of indices to delete
     * @return if the delete was ack'd by the cluster or not
     */
    public boolean deleteIndex(String... indices) {
        logger.trace("in deleteIndex indices:{}", indices);
        ListenableActionFuture<DeleteIndexResponse> action = client.admin().indices().prepareDelete(indices).execute();
        DeleteIndexResponse resp = null;
        boolean acked = false;

        try {
            resp = action.actionGet();
            acked = resp.acknowledged();
        } catch (ElasticSearchException e) {
            logger.error("Error deleting index: {}", indices);
        }

        logger.trace("exit deleteIndex: {}", acked);
        return acked;
    }

    /**
     * Deletes one or more apps
     * 
     * @param apps the list of apps to delete
     * @return if the delete was ack'd by the cluster or not
     */
    public boolean deleteApp(String... apps) {
        logger.trace("in deleteApp apps:{}", apps);
        String[] appsWithSuffix = appsWithSuffix(apps);
        return deleteIndex(appsWithSuffix);
    }

    /**
     * Gets all the mappings for a specified index
     * 
     * @param index the index to get the mappings for
     * @return a map where the key is the type, and the value is the mapping. null when there is an error.
     */
    public Map<String, MappingMetaData> getMappings(String index) {
        logger.trace("in getMappings index:{}", index);
        ClusterState state = getClusterState();
        Map<String, MappingMetaData> mappings = null;

        try {
            mappings = state.metaData().index(index).mappings();
        } catch (Exception e) {
            logger.debug("Error getting mapping for index: {}", index, e);
        }

        logger.trace("exit getMappings: {}", mappings);
        return mappings;
    }

    /**
     * Get types for an application
     * 
     * @param app the name of the application to get the types for
     * @return a map where the key is the type and the value is the type info. null when there is an error.
     */
    public Map<String, MappingMetaData> getAppTypes(String app) {
        logger.trace("in getAppTypes app:{}", app);
        String appIdx = appsWithSuffix(app)[0];
        logger.debug("appIdx: {}", appIdx);
        return getMappings(appIdx);
    }

    /**
     * Alias for getMappings
     * 
     * @param index the index to get the mappings for
     * @return a map where the key is the type and the value is the mapping info. null when there is an error.
     */
    public Map<String, MappingMetaData> getTypes(String index) {
        logger.trace("in getTypes index:{}", index);
        return getMappings(index);
    }

    /**
     * Get a specific type mapping for the specified index
     * 
     * @param index the index to get the type mapping from
     * @param type the type you want the mapping for
     * @return the mapping object or null on error
     */
    public MappingMetaData getMapping(String index, String type) {
        logger.trace("in getMapping index:{}, type:{}", index, type);
        ClusterState state = getClusterState();
        MappingMetaData mapping = null;

        try {
            mapping = state.metaData().index(index).mapping(type);
        } catch (Exception e) {
            logger.debug("Error getting mapping for index:{} and type:{}", new Object[]{index, type}, e);
        }

        logger.trace("exit getMapping:{}", mapping);
        return mapping;
    }

    /**
     * Alias for getMapping
     * 
     * @param index the index to get the type from
     * @param type the type you want
     * @return the type mapping info or null on error
     */
    public MappingMetaData getType(String index, String type) {
        logger.trace("in getType index:{} type:{}", index, type);
        return getMapping(index, type);
    }

    /**
     * Applies a mapping to a given index and type. Does not create index if it does not exist.
     * 
     * @param index the index the type belongs to
     * @param type the type you want to apply the mapping for
     * @param mapping the mapping
     * @return if the mapping was ack'd by the cluster or not
     * @throws MappingException
     */
    public boolean putMapping(String index, String type, String mapping) throws MappingException {
        logger.trace("in putMapping index:{}. type:{}, mapping:{}", new Object[]{index, type, mapping});
        return putMapping(index, type, mapping, false);
    }

    /**
     * Applies a mapping to a given index and type.
     * 
     * @param index the index the type belongs to
     * @param type the type you want to apply the mapping for
     * @param mapping the mapping
     * @param createIndex if you want to create the index if it does not exist
     * @return if the mapping was ack'd by the cluster or not
     * @throws MappingException
     */
    public boolean putMapping(String index, String type, String mapping, boolean createIndex) throws MappingException {
        logger.trace("in putMapping index:{} type:{} mapping:{} createIndex:{}", new Object[]{index, type, mapping, createIndex});
        boolean resp = false;

        boolean exists = hasIndex(index);
        logger.debug("hasIndex:{} createIndex:{}", exists, createIndex);
        try {
            if (!exists && createIndex) {
                Map<String, String> mappings = new HashMap<String, String>();
                mappings.put(type, mapping);
                resp = createIndex(index, mappings);
            } else {
                PutMappingRequest req = new PutMappingRequest();
                req.indices(new String[]{index});
                req.type(type);
                req.source(mapping);

                ActionFuture<PutMappingResponse> action = client.admin().indices().putMapping(req);
                PutMappingResponse mappingResp = action.actionGet();
                resp = mappingResp.acknowledged();
            }
        } catch (Exception e) {
            logger.warn("Error installing mapping for index {} and type {}", new Object[]{index, type}, e);
            throw new MappingException("Error installing mapping", e);
        }

        logger.trace("exit putMapping: {}", resp);
        return resp;
    }

    /**
     * Deletes a mapping
     * 
     * @param index the index to delete the type mapping from
     * @param type the type name of the mapping to delete
     * @return true on success, false on fail
     */
    public boolean deleteMapping(String index, String type) {
        logger.trace("in deleteMapping index:{}, type:{}", index, type);
        boolean resp = false;

        try {
            DeleteMappingRequest req = new DeleteMappingRequest(index);
            req.type(type);
            client.admin().indices().deleteMapping(req).actionGet();
            resp = true;
        } catch (Exception e) {
            logger.debug("Error deleting mapping for index:{} and type:{}", new Object[]{index, type}, e);
        }

        logger.trace("exit getMapping:{}", resp);
        return resp;
    }

    /**
     * Alias for deleteMapping
     * 
     * @param index the index to delete the type from
     * @param type the type to delete
     * @return true on success, false otherwise
     */
    public boolean deleteType(String index, String type) {
        logger.trace("in deleteType index:{}, type:{}", index, type);
        return deleteMapping(index, type);
    }

    /**
     * Creates a type
     * 
     * @param index the index you want to create the type for
     * @param type the name of the type to create
     * @return if the type creation was ack'd by the cluster or not
     * @throws Cloud9Exception
     */
    public boolean createType(String index, String type) throws Cloud9Exception {
        logger.trace("in createType index:{} type:{}", index, type);
        Map<String, MappingMetaData> mappings = getMappings(index);
        boolean resp = false;

        boolean validName = C9Helper.isValidName(type);
        logger.debug("validName:{}", validName);
        if (!validName) {
            throw new TypeCreationException("Invalid type name: " + type);
        }

        logger.debug("mappings:{}", mappings);
        if (mappings == null) {
            throw new IndexMissingException("Index does not exist: " + index);
        }

        if (mappings.containsKey(type)) {
            throw new TypeExistsException("Type already exists: " + type);
        }

        try {
            // put an empty mapping to create the type
            resp = putMapping(index, type, "{\"" + type + "\":{}}");
        } catch (MappingException e) {
            logger.error("Error creating type:{} in index:{}", new Object[]{type, index}, e);
            throw new TypeCreationException("Error creating type: " + type, e);
        }

        logger.trace("exit createType: {}", resp);
        return resp;
    }

    /**
     * Imports an application, defaults: don't force, install mappings
     * 
     * @param app the name of the application to import
     * @param input the input stream of the zip file containing the application's files
     * @throws Cloud9Exception
     */
    public void importApp(String app, InputStream input) throws Cloud9Exception {
        logger.trace("in importApp app:{}, input:{}", new Object[]{app, input});
        importApp(app, input, false, true);
    }

    /**
     * Imports an application, defaults: install mappings
     * 
     * @param app the name of the application to import
     * @param input the input stream of the zip file containing the application's files
     * @param force to force install the application
     * @throws Cloud9Exception
     */
    public void importApp(String app, InputStream input, boolean force) throws Cloud9Exception {
        logger.trace("in importApp app:{}, input:{}, force:{}", new Object[]{app, input, force});
        importApp(app, input, force, true);
    }

    /**
     * Imports an application
     * 
     * @param app the name of the application to import
     * @param input the input stream of the zip file containing the application's files
     * @param force to force install the application
     * @param mappings to install any mappings found in the file or not
     * @throws Cloud9Exception
     */
    @SuppressWarnings("unchecked")
    public void importApp(String app, InputStream input, boolean force, boolean mappings) throws Cloud9Exception {
        logger.trace("in importApp app:{}, input:{}, force:{}, mappings:{}", new Object[]{app, input, force, mappings});
        String sep = System.getProperty("file.separator");
        String appIndex = appsWithSuffix(app)[0];
        logger.debug("sep:{} appIndex:{}", sep, appIndex);

        logger.debug("input: {}", input);
        if (input == null) {
            logger.error("input stream for {} is null", app);
            throw new Cloud9Exception("Error importing app:" + app + ", input stream is null");
        }

        Map<String, IndexStatus> apps = getAppStatus();
        logger.debug("force:{} apps:{}", force, apps.keySet());
        if (!force && apps.containsKey(app)) {
            logger.error("Application already exists: {}, foce to override", app);
            throw new Cloud9Exception("Application already exists: " + app);
        }

        if (force && apps.containsKey(app)) {
            logger.info("Forcing overwrite of {}", app);
            deleteApp(app);
        }

        createAppIndex(app);

        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(new BufferedInputStream(input));
            ZipEntry entry = null;

            while ((entry = zip.getNextEntry()) != null) {
                logger.debug("isDirectory: {}", entry.isDirectory());
                if (entry.isDirectory()) {
                    logger.info("Skipping directory entry: {}", entry.getName());
                    continue;
                }

                String[] pathParts = entry.getName().split(sep);
                logger.debug("pathParts: {}", pathParts);

                logger.debug("number of parts: {}", pathParts.length);
                if (pathParts.length != 3) {
                    logger.warn("Invalid resource: {}", entry.getName());
                    throw new Cloud9Exception("Invalid resource: " + entry.getName());
                }

                String partApp = pathParts[0];
                String partType = pathParts[1];
                String partName = pathParts[2];

                logger.debug("force:{} partApp:{}", force, partApp);
                if (!force && !partApp.equals(app)) {
                    logger.warn("Name mismatch, found {}, expecting {}, use force option to override", partApp, app);
                    throw new Cloud9Exception("Name mismatch, use force?");
                }

                logger.debug("partType: {}", partType);
                if (!Arrays.asList(VALID_TYPES).contains(partType)) {
                    logger.warn("Invalid resource: {}", entry.getName());
                    throw new Cloud9Exception("Invalid resource: " + entry.getName());
                }

                if (partType.equals("conf")) {
                    if (!mappings) {
                        logger.debug("Skipping mapping: {}", entry.getName());
                        continue;
                    }

                    String indexName = partName.replaceAll("\\.json", "");
                    logger.debug("indexName: {}", indexName);
                    JSONObject json = (JSONObject) JSONValue.parse(IOUtils.toString(zip, "UTF-8"));
                    logger.debug("json: {}", json);
                    for (Object k : json.entrySet()) {
                        logger.debug("type: {}", k);
                        Map.Entry<String, JSONObject> type = (Map.Entry<String, JSONObject>) k;
                        JSONObject mapping = new JSONObject();
                        mapping.put(type.getKey(), type.getValue());
                        putMapping(indexName, type.getKey(), mapping.toString(), true);
                    }
                } else if (partType.equals("html")) {
                    indexAppDoc(app, "html", partName, IOUtils.toString(zip, "UTF-8"), "text/html");
                } else if (partType.equals("css")) {
                    indexAppDoc(app, "css", partName, IOUtils.toString(zip, "UTF-8"), "text/css");
                } else if (partType.equals("images")) {
                    int sIdx = partName.indexOf('.');
                    if (sIdx == -1) {
                        logger.warn("Image without extension: {}", partName);
                        throw new Cloud9Exception("Image without extension: " + partName);
                    }
                    String suffix = partName.substring(sIdx + 1, partName.length());
                    indexAppDoc(app, "images", partName, Base64.encodeBase64String(IOUtils.toByteArray(zip)), "image/" + suffix);
                } else if (partType.equals("js")) {
                    indexAppDoc(app, "js", partName, IOUtils.toString(zip, "UTF-8"), "application/javascript");
                } else if (partType.equals("controllers")) {
                    indexAppDoc(app, "controllers", partName, IOUtils.toString(zip, "UTF-8"), "application/javascript");
                } else {
                    logger.warn("Unknown resource type: {}", partType);
                    continue;
                }
            }

            logger.info("Application {} successfully imported", app);
        } catch (Exception e) {
            logger.error("Error importing application: {}", app);
            deleteApp(app);
            throw new Cloud9Exception("Error importing application: " + app, e);
        } finally {
            logger.debug("closing zip");
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    logger.debug("Error closing zip", e);
                }
            }
        }

        logger.trace("exit importApp");
    }

    public void exportApp(String app, OutputStream out, Map<String, String[]> mappings) throws Cloud9Exception {
        logger.trace("in exportApp app:{}, out:{}, mappings:{}", new Object[]{app, out, mappings});
        String sep = System.getProperty("file.separator");
        String appIndex = appsWithSuffix(app)[0];
        logger.debug("sep:{} appIndex:{}", sep, appIndex);

        logger.debug("input: {}", out);
        if (out == null) {
            logger.error("output stream for {} is null", app);
            throw new Cloud9Exception("Error importing app:" + app + ", output stream is null");
        }

        if (!hasIndex(appIndex)) {
            logger.error("Application does not exist: {}", app);
            throw new Cloud9Exception("Application does not exist: " + app);
        }

        SearchResponse response = client.prepareSearch(appIndex).setFilter(matchAllFilter()).setFrom(0).setSize(250).execute()
                .actionGet();

        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(new BufferedOutputStream(out));
            for (SearchHit hit : response.hits().hits()) {
                logger.debug("hit: {}", hit);
                Map<String, Object> fields = hit.sourceAsMap();
                logger.debug("fields: {}", fields);
                String resourcePath = app + sep + hit.type() + sep + hit.id();
                logger.debug("resourcePath: {}", resourcePath);
                zip.putNextEntry(new ZipEntry(resourcePath));
                String code = (String) fields.get("code");
                logger.debug("code: {}", code);
                if (hit.type().equals("images")) {
                    logger.debug("decoding base64");
                    zip.write(Base64.decodeBase64(code));
                } else {
                    zip.write(code.getBytes("UTF-8"));
                }
                zip.closeEntry();
            }

            logger.debug("mappings: {}", mappings);
            if (mappings != null) {
                for (Entry<String, String[]> mapping : mappings.entrySet()) {
                    String exportIndex = mapping.getKey();
                    String[] exportTypes = mapping.getValue();
                    logger.debug("exportIndex: {}", exportIndex);
                    logger.debug("exportTypes: {}", exportTypes);

                    Map<String, Object> exportedMappings = new HashMap<String, Object>();
                    Map<String, MappingMetaData> types = getTypes(exportIndex);
                    if (exportTypes != null) {
                        List<String> exportTypesArray = Arrays.asList(exportTypes);
                        logger.debug("exportTypesArray: {}", exportTypesArray);
                        for (Entry<String, MappingMetaData> type : types.entrySet()) {
                            String typeName = type.getKey();
                            boolean exportType = exportTypesArray.contains(typeName);
                            logger.debug("export type {}: {}", typeName, exportType);
                            if (exportType) {
                                exportedMappings.put(typeName, type.getValue().sourceAsMap());
                            }
                        }
                    } else {
                        for (Entry<String, MappingMetaData> type : types.entrySet()) {
                            exportedMappings.put(type.getKey(), type.getValue().sourceAsMap());
                        }
                    }

                    logger.debug("exportedMappings: {}", exportedMappings);
                    String mappingPath = app + sep + "conf" + sep + exportIndex + ".json";
                    logger.debug("mappingPath: {}", mappingPath);
                    zip.putNextEntry(new ZipEntry(mappingPath));
                    String json = JSONValue.toJSONString(exportedMappings);
                    logger.debug("json: {}", json);
                    zip.write(json.getBytes("UTF-8"));
                    zip.closeEntry();
                }
            }
        } catch (Exception e) {
            logger.error("Error exporting application: {}", app);
            throw new Cloud9Exception("Error exporting application: " + app, e);
        } finally {
            logger.debug("closing zip");
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    logger.debug("Error closing zip", e);
                }
            }
        }

        logger.trace("exit exportApp");
    }

    /**
     * Executes a matchall query against the specified index and type.
     * 
     * @param index the index to search
     * @param type the type to search, null if you want to search all types in the index
     * @param fields the fields to return, null if no fields, empty array for default source
     * @return the search response, null on error
     */
    public SearchResponse matchAll(String index, String type, String[] fields) {
        logger.trace("in matchAll index:{} type:{} fields:{}", new Object[]{index, type, fields});
        SearchRequestBuilder request = client.prepareSearch(index);
        request.setFilter(matchAllFilter());
        request.setFrom(0);
        request.setSize(250); // TODO make this configurable or use scroll search

        logger.debug("type: {}", type);
        if (type != null) {
            request.setTypes(type);
        }

        logger.debug("fields: {}", fields);
        if (fields == null) {
            request.setNoFields();
        } else if (fields.length > 0) {
            request.addFields(fields);
        }

        SearchResponse response = null;
        ListenableActionFuture<SearchResponse> action = request.execute();

        try {
            response = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.debug("Error executing matchAll search", e);
        }

        logger.trace("exit matchAll: {}", response);
        return response;
    }
}
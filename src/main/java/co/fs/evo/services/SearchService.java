package co.fs.evo.services;

import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
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
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.fs.evo.exceptions.EvoException;
import co.fs.evo.exceptions.application.ApplicationExistsException;
import co.fs.evo.exceptions.application.InvalidApplicationNameException;
import co.fs.evo.exceptions.index.IndexCreationException;
import co.fs.evo.exceptions.index.IndexException;
import co.fs.evo.exceptions.index.IndexExistsException;
import co.fs.evo.exceptions.index.IndexMissingException;
import co.fs.evo.exceptions.mapping.MappingException;
import co.fs.evo.exceptions.type.TypeCreationException;
import co.fs.evo.exceptions.type.TypeExistsException;
import co.fs.evo.utils.EvoHelper;

@Service
public class SearchService {

    private static final String SYSTEM_INDEX = "sys";
    public final String APP_INDEX = "app";
    private static final String[] INVALID_INDEX_NAMES = {"css", "js", "img", "partials", "lib"};
    private static final String[] VALID_TYPES = {"conf", "html", "css", "img", "js", "server-side", "partials", "lib"};

    private static final XLogger logger = XLoggerFactory.getXLogger(SearchService.class);
    private Node node;
    private Client client;

    @Autowired
    private ConfigService config;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Initialize and start our ElasticSearch node.
     * 
     * @throws EvoException
     */
    @PostConstruct
    public void booststrap() throws EvoException {
        logger.entry();
        logger.info("Initializing data/search services");
        node = NodeBuilder.nodeBuilder().settings(config.getNodeSettings()).node();
        client = node.client();

        logger.info("Waiting for cluster status");
        ClusterHealthResponse health = getClusterHealth(true);
        logger.info("Cluster Initialized [name:{}, status:{}]", health.clusterName(), health.status());

        setupSystemIndex();
        setupApplicationIndex();
        logger.info("Node is bootstrapped and online");
        logger.exit();
    }

    /**
     * Cleanly shutdown our ElasticSearch node.
     */
    @PreDestroy
    public void shutdown() {
        logger.entry();
        logger.info("Shutdown initiated");
        if (node != null) {
            logger.info("Node is terminating");
            node.close();
        }
        logger.info("Shutdown complete");
        logger.exit();
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
     * Creates the Evo system index
     * 
     * @throws EvoException
     */
    public void setupSystemIndex() throws EvoException {
        logger.entry();
        boolean hasSysIndex = hasIndex(SYSTEM_INDEX);
        logger.debug("hasSysIndex: {}", hasSysIndex);
        if (!hasSysIndex) {
            logger.info("Initializing system accounts");
            try {
                createIndex(SYSTEM_INDEX, 1, 1);
            } catch (IndexException e) {
                logger.error("Error creating system index");
                throw new EvoException("Error creating system index", e);
            }

            String uid = UUID.randomUUID().toString();

            Map<String, Object> source = new HashMap<String, Object>();
            source.put("username", "admin");
            source.put("password", (passwordEncoder != null)
                    ? passwordEncoder.encodePassword(config.get("admin.password"), uid)
                    : config.get("admin.password"));
            source.put("authorities", new String[]{"supervisor"});
            source.put("uid", uid);
            source.put("accountNonExpired", true);
            source.put("accountNonLocked", true);
            source.put("credentialsNonExpired", true);
            source.put("enabled", true);

            indexDoc(SYSTEM_INDEX, "users", "admin", source);
        } else {
            logger.info("Recovering system account information");
        }

        logger.exit();
    }
    
    /**
     * Creates the application index
     * 
     * @throws EvoException
     */
    public void setupApplicationIndex() throws EvoException {
    	logger.entry();
    	boolean hasAppIndex = hasIndex(APP_INDEX);
    	if (!hasAppIndex) {
    		logger.info("Creating application repository");
    		boolean ack = createIndex(APP_INDEX, 1, 1);
    		logger.exit(ack);
    	} else {
    		logger.info("Recovering application data");
    	}
    }

    /**
     * Gets the cluster health
     * 
     * @return the cluster health, null when there is an error
     */
    public ClusterHealthResponse getClusterHealth() {
        return getClusterHealth(false);
    }

    /**
     * Gets the cluster health
     * 
     * @param waitForYellow if we wait for the cluster to be initialized
     * @return the cluster health, null when there is an error
     */
    public ClusterHealthResponse getClusterHealth(boolean waitForYellow) {
        logger.entry(waitForYellow);
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

        logger.exit();
        return resp;
    }

    /**
     * Gets the state of the cluster
     * 
     * @return the cluster state, null when there is an error
     */
    public ClusterState getClusterState() {
        logger.entry();
        ClusterState clusterState = null;
        ListenableActionFuture<ClusterStateResponse> action = client.admin().cluster().prepareState().execute();

        try {
            ClusterStateResponse resp = action.actionGet();
            clusterState = resp.state();
        } catch (ElasticSearchException e) {
            logger.debug("Error getting cluster state");
        }

        logger.exit();
        return clusterState;
    }

    /**
     * Gets the status of an index.
     * 
     * @param indices a list of index names to get the status for
     * @return a map where the key is the index name and the value is the status for that index, null on error
     */
    public Map<String, IndexStatus> getIndexStatus(String... indices) {
        logger.entry((Object) indices);
        Map<String, IndexStatus> indexStatus = null;
        ListenableActionFuture<IndicesStatusResponse> action = client.admin().indices().prepareStatus(indices).execute();

        try {
            IndicesStatusResponse resp = action.actionGet();
            indexStatus = resp.indices();
        } catch (ElasticSearchException e) {
            logger.debug("Error getting index status [{}]", e.getMessage());
        }

        logger.exit();
        return indexStatus;
    }

    /**
     * Gets all the collections and their status.
     * 
     * @param collections the collections to get the status for
     * @return a map where the key is the collection name and the value is the status
     */
    public Map<String, IndexStatus> getCollectionStatus(String... collections) {
        logger.entry((Object) collections);
        Map<String, IndexStatus> collectionStatus = new HashMap<String, IndexStatus>();
        Map<String, IndexStatus> indices = getIndexStatus(collections);

        if (indices != null) {
            logger.debug("indices: {}", indices.keySet());
            for (Entry<String, IndexStatus> index : indices.entrySet()) {
                String indexName = index.getKey();
                IndexStatus indexStatus = index.getValue();
                if (!indexName.equals(SYSTEM_INDEX) && !indexName.equals(APP_INDEX)) {
                    collectionStatus.put(indexName, indexStatus);
                }
            }
        }

        logger.exit();
        return collectionStatus;
    }

    public List<String> getAppNames() {

    	Collection<String> appSet = new HashSet<String>();
    	
    	Map<String, MappingMetaData> appTypes = getMappings(APP_INDEX);
        if (appTypes != null) {
            for (String appType : appTypes.keySet()) {
            	appSet.add(appType.split("_")[0]);
            }
        }
        List<String> appNames = new ArrayList<String>(appSet);
        Collections.sort(appNames);
    	return appNames;
    }

    /**
     * Gets the total number of collection documents that exist in the cluster
     * 
     * @return the sum of all collection document counts.
     */
    public long getTotalCollectionDocCount() {
        logger.entry();
        long numDocs = 0;
        Map<String, IndexStatus> collections = getCollectionStatus();

        logger.debug("collections: {}", collections);
        if (collections != null) {
            for (Entry<String, IndexStatus> collection : collections.entrySet()) {
                numDocs = numDocs + collection.getValue().docs().numDocs();
            }
        }

        logger.exit(numDocs);
        return numDocs;
    }

    /**
     * Get information about nodes in the cluster.
     * 
     * @return a map where the key is the node id and the value is the info for that node, null on error
     */
    public Map<String, NodeInfo> getNodeInfo() {
        logger.entry();
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

        logger.exit();
        return nodeInfo;
    }

    /**
     * Get stats about nodes in the cluster.
     * 
     * @return a map where the key is the node id and the value is the info for that node, null on error
     */
    public Map<String, NodeStats> getNodeStats() {
        logger.entry();
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

        logger.exit();
        return nodeStats;
    }

    /**
     * Checks if the specified index exists or not
     * 
     * @param name the name of the index to check for
     * @return true if exists, false otherwise
     */
    public boolean hasIndex(String name) {
        logger.entry(name);
        boolean exists = false;
        ClusterState state = getClusterState();
        if (state != null) {
            logger.debug("state not null");
            exists = state.metaData().hasIndex(name);
        }

        logger.exit(exists);
        return exists;
    }

    /**
     * Checks if the specified app exists or not
     * 
     * @param appName the name of the app to check for
     * @return true if exists, false otherwise
     */
    public boolean hasApp(String appName) {
        logger.entry(appName);
    	
    	Map<String, MappingMetaData> appTypes = getMappings(APP_INDEX);
        if (appTypes != null) {
            for (String appType : appTypes.keySet()) {
            	if (appType.split("_")[0].equalsIgnoreCase(appName)) {
            		return true;
            	}
            }
        }
        return false;
    }

    /**
     * Creates an index with default settings
     * 
     * @param name the name of the index to create
     * @return if the creation of the index was ack'd by the cluster
     * @throws IndexException
     */
    public boolean createIndex(String name) throws IndexException {
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
        logger.entry(name);
        boolean valid = EvoHelper.isValidName(name);
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

        if (mappings != null) {
            logger.debug("found mappings");
            for (Entry<String, String> mapping : mappings.entrySet()) {
                logger.debug("found mapping for:{}", mapping.getKey());
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

        logger.exit();
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
        logger.entry(name);

        if (name.equals(SYSTEM_INDEX) || name.equals(APP_INDEX) || Arrays.asList(INVALID_INDEX_NAMES).contains(name)) {
            throw new IndexCreationException("Invliad collection name: " + name);
        }

        logger.exit();
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
        logger.entry(name, shards, replicas);

        if (name.equals(SYSTEM_INDEX) || name.equals(APP_INDEX) || Arrays.asList(INVALID_INDEX_NAMES).contains(name)) {
            throw new IndexCreationException("Invliad collection name: " + name);
        }

        logger.exit();
        return createIndex(name, shards, replicas);
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
        logger.entry(index, type, id);
        IndexRequest req = new IndexRequest(index, type, id).source(source);
        ActionFuture<IndexResponse> action = client.index(req);
        IndexResponse resp = null;
        try {
            resp = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.warn("Error indexing document: index:{}, type:{}, id:{}, {}", new Object[]{index, type, id, e.getMessage()});
            logger.debug("exception", e);
        }

        logger.exit();
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
        logger.entry(app, type, id, mime);

        Map<String, Object> source = new HashMap<String, Object>();
        source.put("code", code);
        source.put("mime", mime);

        logger.exit();
        return indexDoc(APP_INDEX, app + "_" + type, id, source);
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
        logger.entry(index, type, id);
        DeleteRequest req = new DeleteRequest(index, type, id);
        ActionFuture<DeleteResponse> action = client.delete(req);
        DeleteResponse resp = null;

        try {
            resp = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.warn("Error delete document: index:{}, type:{}, id:{}", new Object[]{index, type, id});
        }

        logger.exit();
        return resp;
    }

    /**
     * Creates a new application by generating a set of boilerplate mappings.
     * 
     * @param appName the name of the app
     * @throws MappingException 
     * @throws IndexException
     */
    public void createApp(String appName) 
    	throws ApplicationExistsException, MappingException, InvalidApplicationNameException {
        logger.entry(appName);
        
        if (Arrays.asList(INVALID_INDEX_NAMES).contains(appName)) {
        	throw new InvalidApplicationNameException("Invalid application name");
        }
        
        if (!hasApp(appName)) {

        	/* setup application mappings */
			putMapping(APP_INDEX, appName + "_html", config.getHtmlMapping());
			putMapping(APP_INDEX, appName + "_partials", config.getPartialsMapping());
			putMapping(APP_INDEX, appName + "_css", config.getCssMapping());
			putMapping(APP_INDEX, appName + "_js", config.getJsMapping());
			putMapping(APP_INDEX, appName + "_lib", config.getLibMapping());
			putMapping(APP_INDEX, appName + "_img", config.getImagesMapping());
			putMapping(APP_INDEX, appName + "_server-side", config.getServerSideMapping());
			
			/* load the HTML application boilerplate */
	        indexAppDoc(appName, "html", "index.html", config.getAngularTemplate(appName, "index.html"), "text/html");
	        indexAppDoc(appName, "partials", "search.html", config.getAngularTemplate(appName, "partial1.html"), "text/html");
	        indexAppDoc(appName, "partials", "results.html", config.getAngularTemplate(appName, "partial2.html"), "text/html");
	        
	        /* load the JavaScript application boilerplate */
	        indexAppDoc(appName, "js", "app.js", config.getAngularTemplate(appName, "app.js"), "application/javascript");
	        indexAppDoc(appName, "js", "controllers.js", config.getAngularTemplate(appName, "controllers.js"), "application/javascript");
	        indexAppDoc(appName, "js", "services.js", config.getAngularTemplate(appName, "services.js"), "application/javascript");
	        indexAppDoc(appName, "js", "directives.js", config.getAngularTemplate(appName, "directives.js"), "application/javascript");
	        indexAppDoc(appName, "js", "filters.js", config.getAngularTemplate(appName, "filters.js"), "application/javascript");
	        
	        /* load the JavaScript libraries/dependencies */
	        indexAppDoc(appName, "lib", "json2.min.js", config.getAngularResource("js/json2.min.js"), "application/javascript");
	        indexAppDoc(appName, "lib", "jquery-1.8.0.min.js", config.getAngularResource("js/jquery-1.8.0.min.js"), "application/javascript");
	        indexAppDoc(appName, "lib", "modernizr-2.6.1.min.js", config.getAngularResource("js/modernizr-2.6.1.min.js"), "application/javascript");
	        indexAppDoc(appName, "lib", "underscore.min.js", config.getAngularResource("js/underscore.min.js"), "application/javascript");
	        indexAppDoc(appName, "lib", "bootstrap.min.js", config.getAngularResource("js/bootstrap.min.js"), "application/javascript");
	        indexAppDoc(appName, "lib", "angular.min.js", config.getAngularResource("js/angular.min.js"), "application/javascript");
	        indexAppDoc(appName, "lib", "evo.min.js", config.getAngularResource("js/evo.min.js"), "application/javascript");
	        
	        /* load the CSS application boilerplate */
	        indexAppDoc(appName, "css", appName+".css", config.getAngularTemplate(appName, "project.css"), "text/css");
	        indexAppDoc(appName, "css", "bootstrap.min.css", config.getAngularResource("css/bootstrap.min.css"), "text/css");
	        indexAppDoc(appName, "css", "bootstrap-responsive.min.css", config.getAngularResource("css/bootstrap-responsive.min.css"), "text/css");
	        indexAppDoc(appName, "css", "normalize.min.css", config.getAngularResource("css/normalize.min.css"), "text/css");
	        indexAppDoc(appName, "css", "main.min.css", config.getAngularResource("css/main.min.css"), "text/css");
	        
	        /* load the SSJS example boilerplate */
	        indexAppDoc(appName, "server-side", "examples.js", config.getAngularTemplate(appName, "examples.js"), "application/javascript");
	        
	        /* load application images */
	        indexAppDoc(appName, "img", "glyphicons-halflings.png", config.getBase64Image("img/glyphicons-halflings.png"), "image/png");
	        indexAppDoc(appName, "img", "glyphicons-halflings-white.png", config.getBase64Image("img/glyphicons-halflings-white.png"), "image/png");

        } else {
        	throw new ApplicationExistsException("Application already exists");
        }
        logger.exit();
    }

    /**
     * Refreshes the specified indices
     * 
     * @param indices the indices to refresh
     * @return the refresh response, null on error
     */
    public RefreshResponse refreshIndex(String... indices) {
        logger.entry((Object) indices);
        RefreshResponse refreshResponse = null;
        ListenableActionFuture<RefreshResponse> action = client.admin().indices().prepareRefresh(indices).execute();

        try {
            refreshResponse = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.debug("Error refreshing indices:{}", indices);
        }

        logger.exit();
        return refreshResponse;
    }

    /**
     * Deletes one or more index
     * 
     * @param indices the list of indices to delete
     * @return if the delete was ack'd by the cluster or not
     */
    public boolean deleteIndex(String... indices) {
        logger.entry((Object) indices);
        ListenableActionFuture<DeleteIndexResponse> action = client.admin().indices().prepareDelete(indices).execute();
        DeleteIndexResponse resp = null;
        boolean acked = false;

        try {
            resp = action.actionGet();
            acked = resp.acknowledged();
        } catch (ElasticSearchException e) {
            logger.error("Error deleting index: {}", indices);
        }

        logger.exit(acked);
        return acked;
    }

    /**
     * Deletes one or more apps
     * 
     * @param apps the list of apps to delete
     * @return if the delete was ack'd by the cluster or not
     */
    public boolean deleteApp(String... apps) {
        for (int appIdx = 0; appIdx < apps.length; appIdx++) {
        	try {
				List<String> types = getAppTypes(apps[appIdx]);
				for (String type: types) {
					deleteMapping(APP_INDEX, apps[appIdx] + "_" + type);
				}
			} catch (InvalidApplicationNameException e) {
				logger.warn("Encountered invalid app name trying to delete an application");
				continue;
			}
        }
        return true;
    }

    /**
     * Gets all the mappings for a specified index
     * 
     * @param index the index to get the mappings for
     * @return a map where the key is the type, and the value is the mapping. null when there is an error.
     */
    public Map<String, MappingMetaData> getMappings(String index) {
        logger.entry(index);
        ClusterState state = getClusterState();
        Map<String, MappingMetaData> mappings = null;

        try {
            mappings = state.metaData().index(index).mappings();
        } catch (Exception e) {
            logger.debug("Error getting mapping for index: {} [{}]", index, e.getMessage());
        }

        logger.exit();
        return mappings;
    }

    /**
     * Get types for an application
     * 
     * @param app the name of the application to get the types for
     * @return a sorted list of application names
     */
    public List<String> getAppTypes(String app) throws InvalidApplicationNameException {

    	List<String> resp = new ArrayList<String>();
    	
    	Map<String, MappingMetaData> appTypes = getMappings(APP_INDEX);
        if (appTypes != null) {
            for (String appType : appTypes.keySet()) {
            	String[] parts = appType.split("_");
            	if (parts.length != 2) {
            		throw new InvalidApplicationNameException("Invalid application name: " + app);
            	}
                
                if (app.equalsIgnoreCase(parts[0])) {
                    logger.debug("adding appType: {}", parts[1]);
                	resp.add(parts[1]);
                }
            }
        }
        Collections.sort(resp);
        return resp;
    }

    /**
     * Alias for getMappings
     * 
     * @param index the index to get the mappings for
     * @return a map where the key is the type and the value is the mapping info. null when there is an error.
     */
    public Map<String, MappingMetaData> getTypes(String index) {
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
        logger.entry(index, type);
        ClusterState state = getClusterState();
        MappingMetaData mapping = null;

        try {
            mapping = state.metaData().index(index).mapping(type);
        } catch (Exception e) {
            logger.debug("Error getting mapping for index:{} and type:{}", new Object[]{index, type}, e);
        }

        logger.exit();
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
        logger.entry(index, type, createIndex);
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

        logger.exit(resp);
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
        logger.entry(index, type);
        boolean resp = false;

        try {
            DeleteMappingRequest req = new DeleteMappingRequest(index);
            req.type(type);
            client.admin().indices().deleteMapping(req).actionGet();
            resp = true;
        } catch (Exception e) {
            logger.debug("Error deleting mapping for index:{} and type:{} [{}]", new Object[]{index, type, e.getMessage()});
        }

        logger.exit(resp);
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
        return deleteMapping(index, type);
    }

    /**
     * Creates a type
     * 
     * @param index the index you want to create the type for
     * @param type the name of the type to create
     * @return if the type creation was ack'd by the cluster or not
     * @throws EvoException
     */
    public boolean createType(String index, String type) throws EvoException {
        logger.entry(index, type);
        Map<String, MappingMetaData> mappings = getMappings(index);
        boolean resp = false;

        boolean validName = EvoHelper.isValidName(type);
        logger.debug("validName:{}", validName);
        if (!validName) {
            throw new TypeCreationException("Invalid type name: " + type);
        }

        if (mappings == null) {
            logger.debug("mappings is null");
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

        logger.exit(resp);
        return resp;
    }

    /**
     * Imports an application, defaults: don't force, install mappings
     * 
     * @param app the name of the application to import
     * @param input the input stream of the zip file containing the application's files
     * @throws EvoException
     */
    public void importApp(String app, InputStream input) throws EvoException {
        importApp(app, input, false, true);
    }

    /**
     * Imports an application, defaults: install mappings
     * 
     * @param app the name of the application to import
     * @param input the input stream of the zip file containing the application's files
     * @param force to force install the application
     * @throws EvoException
     */
    public void importApp(String app, InputStream input, boolean force) throws EvoException {
        importApp(app, input, force, true);
    }

    /**
     * Imports an application
     * 
     * @param app the name of the application to import
     * @param input the input stream of the zip file containing the application's files
     * @param force to force install the application
     * @param mappings to install any mappings found in the file or not
     * @throws EvoException
     */
    @SuppressWarnings("unchecked")
    public void importApp(String app, InputStream input, boolean force, boolean mappings) throws EvoException {
        logger.entry(app, force, mappings);
        String sep = System.getProperty("file.separator");
        logger.debug("sep:{} appIndex:{}", sep, app);

        if (input == null) {
            logger.error("input stream for {} is null", app);
            throw new EvoException("Error importing app:" + app + ", input stream is null");
        }

        if (!force && hasApp(app)) {
            logger.error("Application already exists: {}, foce to override", app);
            throw new EvoException("Application already exists: " + app);
        }

        if (force && hasApp(app)) {
            logger.info("Forcing overwrite of {}", app);
            deleteApp(app);
        }

        createApp(app);

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
                	if (pathParts.length == 2) {
                		// the html dir isn't specified in most cases
                	    String[] newParts = {pathParts[0], "html", pathParts[1]};
                        pathParts = newParts;
                	} else {
                		logger.warn("Invalid resource: {}", entry.getName());
                		throw new EvoException("Invalid resource: " + entry.getName());
                	}
                }

                String partApp = pathParts[0];
                String partType = pathParts[1];
                String partName = pathParts[2];

                logger.debug("force:{} partApp:{}", force, partApp);
                if (!force && !partApp.equals(app)) {
                    logger.warn("Name mismatch, found {}, expecting {}, use force option to override", partApp, app);
                    throw new EvoException("Name mismatch, use force?");
                }

                logger.debug("partType: {}", partType);
                if (!Arrays.asList(VALID_TYPES).contains(partType)) {
                    logger.warn("Invalid resource: {}", entry.getName());
                    throw new EvoException("Invalid resource: " + entry.getName());
                }

                if (partType.equals("conf")) {
                    if (!mappings) {
                        logger.debug("Skipping mapping: {}", entry.getName());
                        continue;
                    }

                    String indexName = partName.replaceAll("\\.json", "");
                    logger.debug("indexName: {}", indexName);
                    JSONObject json = (JSONObject) JSONValue.parse(IOUtils.toString(zip, "UTF-8"));
                    for (Object k : json.entrySet()) {
                        Map.Entry<String, JSONObject> type = (Map.Entry<String, JSONObject>) k;
                        logger.debug("found mapping for type: {}", type.getKey());
                        JSONObject mapping = new JSONObject();
                        mapping.put(type.getKey(), type.getValue());
                        putMapping(indexName, type.getKey(), mapping.toString(), true);
                    }
                } else if (partType.equals("html")) {
                    indexAppDoc(app, "html", partName, IOUtils.toString(zip, "UTF-8"), "text/html");
                } else if (partType.equals("partials")) {
                    indexAppDoc(app, "partials", partName, IOUtils.toString(zip, "UTF-8"), "text/html");
                } else if (partType.equals("lib")) { 
                	indexAppDoc(app, "lib", partName, IOUtils.toString(zip, "UTF-8"), "application/javascript");
            	}else if (partType.equals("css")) {
                    indexAppDoc(app, "css", partName, IOUtils.toString(zip, "UTF-8"), "text/css");
                } else if (partType.equals("img")) {
                    int sIdx = partName.indexOf('.');
                    if (sIdx == -1) {
                        logger.warn("Image without extension: {}", partName);
                        throw new EvoException("Image without extension: " + partName);
                    }
                    String suffix = partName.substring(sIdx + 1, partName.length());
                    indexAppDoc(app, "img", partName, Base64.encodeBase64String(IOUtils.toByteArray(zip)), "image/" + suffix);
                } else if (partType.equals("js")) {
                    indexAppDoc(app, "js", partName, IOUtils.toString(zip, "UTF-8"), "application/javascript");
                } else if (partType.equals("server-side")) {
                    indexAppDoc(app, "server-side", partName, IOUtils.toString(zip, "UTF-8"), "application/javascript");
                } else {
                    logger.warn("Unknown resource type: {}", partType);
                    continue;
                }
            }

            logger.debug("Application {} successfully imported", app);
        } catch (Exception e) {
            logger.error("Error importing application: {}", app);
            deleteApp(app);
            throw new EvoException("Error importing application: " + app, e);
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

        logger.exit();
    }

    public void exportApp(String app, OutputStream out, Map<String, String[]> mappings) throws EvoException {
        logger.entry(app);
        String sep = System.getProperty("file.separator");
        logger.debug("sep:{} appIndex:{}", sep, app);

        if (out == null) {
            logger.error("output stream for {} is null", app);
            throw new EvoException("Error importing app:" + app + ", output stream is null");
        }

        if (!hasApp(app)) {
            logger.error("Application does not exist: {}", app);
            throw new EvoException("Application does not exist: " + app);
        }

        List<String> contentTypes = getAppTypes(app);
        String[] appTypes = new String[contentTypes.size()];
        int idx = 0;
        for (String type: contentTypes) {
        	appTypes[idx] = app + "_" + type;
        	idx++;
        }
        
        SearchResponse response = matchAll(APP_INDEX, appTypes, new String[]{});

        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(new BufferedOutputStream(out));
            for (SearchHit hit : response.hits().hits()) {
                Map<String, Object> fields = hit.sourceAsMap();
                String contentType = hit.type().split("_")[1];
                String resourcePath = "";
                if (contentType.equals("html")) {
                	resourcePath = app + sep + hit.id();
                } else {
                	resourcePath = app + sep + contentType + sep + hit.id();
                }
                logger.debug("resourcePath: {}", resourcePath);
                zip.putNextEntry(new ZipEntry(resourcePath));
                String code = (String) fields.get("code");
                if (contentType.equals("img")) {
                    logger.debug("decoding base64");
                    zip.write(Base64.decodeBase64(code));
                } else {
                    zip.write(code.getBytes("UTF-8"));
                }
                zip.closeEntry();
            }

            if (mappings != null) {
                logger.debug("mappings not null");
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

                    String mappingPath = app + sep + "conf" + sep + exportIndex + ".json";
                    logger.debug("mappingPath: {}", mappingPath);
                    zip.putNextEntry(new ZipEntry(mappingPath));
                    String json = JSONValue.toJSONString(exportedMappings);
                    zip.write(json.getBytes("UTF-8"));
                    zip.closeEntry();
                }
            }
        } catch (Exception e) {
            logger.error("Error exporting application: {}", app);
            throw new EvoException("Error exporting application: " + app, e);
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

        logger.exit();
    }

    public SearchResponse matchAll(String index, String type, String[] fields) {
    	String[] types = {type};
    	return matchAll(index, types, fields);
    }
    
    /**
     * Executes a matchall query against the specified index and type.
     * 
     * @param index the index to search
     * @param type the type to search, null if you want to search all types in the index
     * @param fields the fields to return, null if no fields, empty array for default source
     * @return the search response, null on error
     */
    public SearchResponse matchAll(String index, String[] type, String[] fields) {
        logger.entry(index, type, fields);
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

        logger.exit();
        return response;
    }

    /**
     * Gets a specific document
     * 
     * @param index the name of the index where the document exists
     * @param type the name of the type the document belongs to
     * @param id the id of the document you want
     * @param fields a list of fields you want returned, null for the default _source field
     * @return the get response, null on error
     */
    public GetResponse getDoc(String index, String type, String id, String[] fields) {
        logger.entry(index, type, id, fields);
        GetRequestBuilder request = client.prepareGet(index, type, id);

        logger.debug("fields: {}", fields);
        if (fields != null) {
            request.setFields(fields);
        }

        GetResponse response = null;
        ListenableActionFuture<GetResponse> action = request.execute();

        try {
            response = action.actionGet();
        } catch (ElasticSearchException e) {
            logger.debug("Error executing get", e);
        }

        logger.exit();
        return response;
    }

    /**
     * Gets a resource from an app
     * 
     * @param app the appname
     * @param dir the resoruce type/directory
     * @param resource the resource name/id
     * @param fields a list of fields you want returned, null for the default _source field
     * @return the get response, null on error
     */
    public GetResponse getAppResource(String app, String dir, String resource, String[] fields) {
        return getDoc(APP_INDEX, app + "_" + dir, resource, fields);
    }

    /**
     * Get's the local node id. Not sure we will need this, in most cases you can use the "_local" alias.
     * 
     * @return the nodeId
     */
    public String getNodeId() {
        String nodeId = ((InternalNode) node).injector().getInstance(ClusterService.class).state().nodes().localNodeId();
        return nodeId;
    }

    /**
     * Get's the network address we want nodes to publish/communicate on.
     * 
     * @return the ip address, returns localhost (127.0.0.1) when unable to retreive the publish address
     */
    public String getPublishAddress() {
        logger.entry();
        String publishAddress = "127.0.0.1";
        ActionFuture<NodesInfoResponse> action = client.admin().cluster().nodesInfo(new NodesInfoRequest("_local").transport(true));

        try {
            NodesInfoResponse resp = action.actionGet();

            // there should only be 1 node returned, so grab first item
            TransportAddress address = (InetSocketTransportAddress) resp.getAt(0).getTransport().address().publishAddress();
            if (address instanceof InetSocketTransportAddress) {
                logger.debug("found: {}", address);
                publishAddress = ((InetSocketTransportAddress) address).address().getAddress().getHostAddress();
            }
        } catch (ElasticSearchException e) {
            logger.debug("Error getting publish address", e);
        }

        logger.exit(publishAddress);
        return publishAddress;
    }
}
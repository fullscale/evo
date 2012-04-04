package co.diji.cloud9.services;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.cluster.ClusterState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.exceptions.index.IndexCreationException;
import co.diji.cloud9.exceptions.index.IndexExistsException;

public class SearchServiceTest {

    private static SearchService searchService;
    private static ConfigService config;

    @BeforeClass
    public static void setup() throws Cloud9Exception {
        searchService = new SearchService();
        config = new ConfigService();

        // mock appplication context and inject into search service
        MockServletContext servletContext = new MockServletContext();
        WebApplicationContext webappContext = new GenericWebApplicationContext(servletContext);
        ReflectionTestUtils.setField(searchService, "applicationContext", webappContext, WebApplicationContext.class);
        ReflectionTestUtils.setField(config, "applicationContext", webappContext, WebApplicationContext.class);
        ReflectionTestUtils.setField(searchService, "config", config, ConfigService.class);
        System.setProperty("c9.cluster.name", "c9.test.cluster");
        System.setProperty("c9.node.name", "c9.test.node");
        config.init();
        searchService.booststrap();
    }

    @AfterClass
    public static void shutdown() {
        if (searchService != null) {
            searchService.shutdown();
        }
    }

    @Test
    public void testSearchService() {
        assertNotNull(searchService);
    }

    @Test
    public void testClientNotNull() {
        assertNotNull(searchService.getClient());
    }

    @Test
    public void testNodeNotNull() {
        assertNotNull(searchService.getNode());
    }

    @Test
    public void testHealth() {
        ClusterHealthResponse health = searchService.getClusterHealth(true);
        assertNotNull(health);
        assertEquals("c9.test.cluster", health.getClusterName());
        assertEquals(1, health.getNumberOfNodes());
    }

    @Test
    public void testState() {
        ClusterState state = searchService.getClusterState();
        assertNotNull(state);
        assertEquals(1, state.getNodes().size());
    }

    @Test
    public void testIndexStatus() {
        // TODO add more tests once we have more index operations such as create and delete
        Map<String, IndexStatus> indexStatus = searchService.getIndexStatus("doesnotexistindex");
        assertNull(indexStatus);
        indexStatus = searchService.getIndexStatus();
        assertNotNull(indexStatus);
        assertEquals(1, indexStatus.size());
    }

    @Test
    public void testNodeInfo() {
        Map<String, NodeInfo> info = searchService.getNodeInfo();
        assertNotNull(info);
        assertEquals(1, info.size());
        assertEquals("c9.test.node", info.values().iterator().next().getNode().name());
    }

    @Test
    public void testNodeStats() {
        Map<String, NodeStats> stats = searchService.getNodeStats();
        assertNotNull(stats);
        assertEquals(1, stats.size());
        assertEquals("c9.test.node", stats.values().iterator().next().getNode().name());
    }

    @Test
    public void testCreateIndex() throws Exception {
        ClusterIndexHealth index = null;

        assertFalse(searchService.hasIndex("doesnotexist"));

        searchService.createIndex("exists");
        searchService.refreshIndex("exists");
        index = searchService.getClusterHealth().indices().get("exists");
        assertTrue(searchService.hasIndex("exists"));
        assertEquals(config.getNodeSettings().getAsInt("index.number_of_shards", null).intValue(), index.numberOfShards());
        assertEquals(config.getNodeSettings().getAsInt("index.number_of_replicas", null).intValue(), index.numberOfReplicas());

        try {
            searchService.createIndex("exists");
            fail();
        } catch (IndexExistsException e) {
        };

        try {
            searchService.createIndex("BADNAME");
            fail();
        } catch (IndexCreationException e) {
        }
        assertFalse(searchService.hasIndex("BADNAME"));

        searchService.createIndex("oneshardnoreplicas", 1, 0);
        searchService.refreshIndex("oneshardnoreplicas");
        index = searchService.getClusterHealth().indices().get("oneshardnoreplicas");
        assertTrue(searchService.hasIndex("oneshardnoreplicas"));
        assertEquals(1, index.numberOfShards());
        assertEquals(0, index.numberOfReplicas());

        searchService.createIndex("oneshardonereplicas", 1, 1);
        searchService.refreshIndex("oneshardonereplicas");
        index = searchService.getClusterHealth().indices().get("oneshardonereplicas");
        assertTrue(searchService.hasIndex("oneshardonereplicas"));
        assertEquals(1, index.numberOfShards());
        assertEquals(1, index.numberOfReplicas());

        searchService.createIndex("twoshardssixreplicas", 2, 6);
        searchService.refreshIndex("twoshardssixreplicas");
        index = searchService.getClusterHealth().indices().get("twoshardssixreplicas");
        assertTrue(searchService.hasIndex("twoshardssixreplicas"));
        assertEquals(2, index.numberOfShards());
        assertEquals(6, index.numberOfReplicas());

        try {
            searchService.createIndex("validnamebadcounts", -5, -1);
            fail();
        } catch (IndexCreationException e) {
        }
        assertFalse(searchService.hasIndex("validnamebadcounts"));

        // TODO check mappings created successfully
        Map<String, String> mappings = new HashMap<String, String>();
        mappings.put("html", config.getHtmlMapping());

        searchService.createIndex("indexwithhtmlmapping", mappings);
        searchService.refreshIndex("indexwithhtmlmapping");
        index = searchService.getClusterHealth().indices().get("indexwithhtmlmapping");
        assertTrue(searchService.hasIndex("indexwithhtmlmapping"));

        mappings.put("css", config.getCssMapping());
        searchService.createIndex("indexwithcssmapping", mappings);
        searchService.refreshIndex("indexwithcssmapping");
        index = searchService.getClusterHealth().indices().get("indexwithcssmapping");
        assertTrue(searchService.hasIndex("indexwithcssmapping"));

        mappings.put("bad", "bad.junk.mapping");
        try {
            searchService.createIndex("indexbadmapping", mappings);
            fail();
        } catch (IndexCreationException e) {
        }
        assertFalse(searchService.hasIndex("indexbadmapping"));
    }

    @Test
    public void testCreateCollection() throws Exception {
        // we just need to test that we cant create system
        // index or a collection that ends with .app
        try {
            searchService.createCollectionIndex("sys");
            fail();
        } catch (IndexCreationException e) {
        }

        try {
            searchService.createCollectionIndex("sys", 3, 3);
            fail();
        } catch (IndexCreationException e) {
        }

        try {
            searchService.createCollectionIndex("whatever.app");
            fail();
        } catch (IndexCreationException e) {
        }

        try {
            searchService.createCollectionIndex("whatever.app", 2, 1);
            fail();
        } catch (IndexCreationException e) {
        }
    }

    @Test
    public void testCreateAppIndex() throws Exception {
        ClusterIndexHealth index = null;

        assertFalse(searchService.hasIndex("testapp.app"));

        searchService.createAppIndex("testapp");
        searchService.refreshApp("testapp");
        index = searchService.getClusterHealth().indices().get("testapp.app");
        assertTrue(searchService.hasIndex("testapp.app"));
        assertTrue(searchService.hasApp("testapp"));
        assertEquals(1, index.numberOfShards());
        assertEquals(1, index.numberOfReplicas());

        try {
            searchService.createAppIndex("testapp");
            fail();
        } catch (IndexExistsException e) {
        }

        searchService.createAppIndex("testapp2", 3, 1);
        searchService.refreshApp("testapp2");
        index = searchService.getClusterHealth().indices().get("testapp2.app");
        assertTrue(searchService.hasApp("testapp2.app"));
        assertEquals(3, index.numberOfShards());
        assertEquals(1, index.numberOfReplicas());

        try {
            searchService.createAppIndex("css");
            fail();
        } catch (IndexCreationException e) {
        }
        assertFalse(searchService.hasApp("css"));

        try {
            searchService.createAppIndex("js");
            fail();
        } catch (IndexCreationException e) {
        }
        assertFalse(searchService.hasApp("js"));

        try {
            searchService.createAppIndex("images");
            fail();
        } catch (IndexCreationException e) {
        }
        assertFalse(searchService.hasApp("images"));

        searchService.createAppIndex("anotherapp.app");
        searchService.refreshApp("anotherapp");
        assertTrue(searchService.hasApp("anotherapp"));
        assertFalse(searchService.hasIndex("anotherapp.app.app"));

    }

    @Test
    public void testGetCollectionStatus() {
        // all these collections should have been created in tests above
        String[] collections = {
                "exists", "oneshardnoreplicas", "oneshardonereplicas", "twoshardssixreplicas", "indexwithhtmlmapping",
                "indexwithcssmapping"};
        Map<String, IndexStatus> collectionStatus = searchService.getCollectionStatus();
        assertEquals(collections.length, collectionStatus.size());
        collectionStatus = searchService.getCollectionStatus("does.not.exist");
        assertEquals(0, collectionStatus.size());
        collectionStatus = searchService.getCollectionStatus("exists");
        assertEquals(1, collectionStatus.size());
        collectionStatus = searchService.getCollectionStatus(collections);
        assertEquals(collections.length, collectionStatus.size());
        collectionStatus = searchService.getCollectionStatus("exists", "does.not.exist");
        assertEquals(0, collectionStatus.size());
    }

    @Test
    public void testGetAppStatus() {
        // all these collections should have been created in tests above
        String[] apps = {"testapp", "testapp2", "anotherapp"};
        Map<String, IndexStatus> appStatus = searchService.getAppStatus();
        assertEquals(apps.length, appStatus.size());
        appStatus = searchService.getAppStatus("does.not.exist");
        assertEquals(0, appStatus.size());
        appStatus = searchService.getAppStatus("testapp");
        assertEquals(1, appStatus.size());
        appStatus = searchService.getAppStatus(apps);
        assertEquals(apps.length, appStatus.size());
        appStatus = searchService.getAppStatus("testapp", "does.not.exist");
        assertEquals(0, appStatus.size());
    }

    @Test
    public void testGetTotalCollectionDocCount() {
        // TODO add more tests once we can add/remove docs
        long cnt = searchService.getTotalCollectionDocCount();
        assertEquals(0, cnt);
    }

    @Test
    public void testGetTotalAppDocCount() {
        // TODO add more tests once we can add/remove docs
        long cnt = searchService.getTotalAppDocCount();
        assertEquals(0, cnt);
    }

    @Test
    public void testCreateApp() throws Exception {
        ClusterIndexHealth index = null;
        Map<String, IndexStatus> indexStatus = null;

        assertFalse(searchService.hasApp("appwithdocs"));

        searchService.createApp("appwithdocs");
        searchService.refreshApp("appwithdocs");
        index = searchService.getClusterHealth().indices().get("appwithdocs.app");
        indexStatus = searchService.getAppStatus("appwithdocs");
        assertTrue(searchService.hasIndex("appwithdocs.app"));
        assertTrue(searchService.hasApp("appwithdocs"));
        assertEquals(1, index.numberOfShards());
        assertEquals(1, index.numberOfReplicas());
        assertEquals(1, indexStatus.size());
        assertEquals(4, indexStatus.get("appwithdocs").docs().getNumDocs());

        try {
            searchService.createAppIndex("appwithdocs");
            fail();
        } catch (IndexExistsException e) {
        }
    }
}

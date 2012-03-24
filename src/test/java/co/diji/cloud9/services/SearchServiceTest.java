package co.diji.cloud9.services;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

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

import co.diji.cloud9.exceptions.index.IndexCreationException;
import co.diji.cloud9.exceptions.index.IndexExistsException;

public class SearchServiceTest {

    private static SearchService searchService;
    private static ConfigService config;

    @BeforeClass
    public static void setup() {
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
        Assert.assertNotNull(searchService);
    }

    @Test
    public void testClientNotNull() {
        Assert.assertNotNull(searchService.getClient());
    }

    @Test
    public void testNodeNotNull() {
        Assert.assertNotNull(searchService.getNode());
    }

    @Test
    public void testHealth() {
        ClusterHealthResponse health = searchService.getClusterHealth();
        Assert.assertNotNull(health);
        Assert.assertEquals("c9.test.cluster", health.getClusterName());
        Assert.assertEquals(1, health.getNumberOfNodes());
    }

    @Test
    public void testState() {
        ClusterState state = searchService.getClusterState();
        Assert.assertNotNull(state);
        Assert.assertEquals(1, state.getNodes().size());
    }

    @Test
    public void testIndexStatus() {
        // TODO add more tests once we have more index operations such as create and delete
        Map<String, IndexStatus> indexStatus = searchService.getIndexStatus("doesnotexistindex");
        Assert.assertNull(indexStatus);
        indexStatus = searchService.getIndexStatus();
        Assert.assertNotNull(indexStatus);
        Assert.assertEquals(0, indexStatus.size());
    }

    @Test
    public void testNodeInfo() {
        Map<String, NodeInfo> info = searchService.getNodeInfo();
        Assert.assertNotNull(info);
        Assert.assertEquals(1, info.size());
        Assert.assertEquals("c9.test.node", info.values().iterator().next().getNode().name());
    }

    @Test
    public void testNodeStats() {
        Map<String, NodeStats> stats = searchService.getNodeStats();
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.size());
        Assert.assertEquals("c9.test.node", stats.values().iterator().next().getNode().name());
    }

    @Test
    public void testCreateIndex() throws Exception {
        ClusterIndexHealth index = null;

        Assert.assertFalse(searchService.hasIndex("doesnotexist"));

        searchService.createIndex("exists");
        index = searchService.getClusterHealth().indices().get("exists");
        Assert.assertTrue(searchService.hasIndex("exists"));
        Assert.assertEquals(config.getNodeSettings().getAsInt("index.number_of_shards", null).intValue(), index.numberOfShards());
        Assert.assertEquals(config.getNodeSettings().getAsInt("index.number_of_replicas", null).intValue(),
                index.numberOfReplicas());

        try {
            searchService.createIndex("exists");
            Assert.fail();
        } catch (IndexExistsException e) {
        };

        try {
            searchService.createIndex("BADNAME");
            Assert.fail();
        } catch (IndexCreationException e) {
        }
        Assert.assertFalse(searchService.hasIndex("BADNAME"));

        searchService.createIndex("oneshardnoreplicas", 1, 0);
        index = searchService.getClusterHealth().indices().get("oneshardnoreplicas");
        Assert.assertTrue(searchService.hasIndex("oneshardnoreplicas"));
        Assert.assertEquals(1, index.numberOfShards());
        Assert.assertEquals(0, index.numberOfReplicas());

        searchService.createIndex("oneshardonereplicas", 1, 1);
        index = searchService.getClusterHealth().indices().get("oneshardonereplicas");
        Assert.assertTrue(searchService.hasIndex("oneshardonereplicas"));
        Assert.assertEquals(1, index.numberOfShards());
        Assert.assertEquals(1, index.numberOfReplicas());

        searchService.createIndex("twoshardssixreplicas", 2, 6);
        index = searchService.getClusterHealth().indices().get("twoshardssixreplicas");
        Assert.assertTrue(searchService.hasIndex("twoshardssixreplicas"));
        Assert.assertEquals(2, index.numberOfShards());
        Assert.assertEquals(6, index.numberOfReplicas());

        try {
            searchService.createIndex("validnamebadcounts", -5, -1);
            Assert.fail();
        } catch (IndexCreationException e) {
        }
        Assert.assertFalse(searchService.hasIndex("validnamebadcounts"));

        // TODO check mappings created successfully
        Map<String, String> mappings = new HashMap<String, String>();
        mappings.put("html", config.getHtmlMapping());

        searchService.createIndex("indexwithhtmlmapping", mappings);
        index = searchService.getClusterHealth().indices().get("indexwithhtmlmapping");
        Assert.assertTrue(searchService.hasIndex("indexwithhtmlmapping"));

        mappings.put("css", config.getCssMapping());
        searchService.createIndex("indexwithcssmapping", mappings);
        index = searchService.getClusterHealth().indices().get("indexwithcssmapping");
        Assert.assertTrue(searchService.hasIndex("indexwithcssmapping"));

        mappings.put("bad", "bad.junk.mapping");
        try {
            searchService.createIndex("indexbadmapping", mappings);
            Assert.fail();
        } catch (IndexCreationException e) {
        }
        Assert.assertFalse(searchService.hasIndex("indexbadmapping"));
    }
}

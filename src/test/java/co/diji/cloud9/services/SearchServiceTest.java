package co.diji.cloud9.services;

import java.util.Map;

import junit.framework.Assert;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

public class SearchServiceTest {

    private static SearchService searchService;

    @BeforeClass
    public static void setup() {
        searchService = new SearchService();

        // mock appplication context and inject into search service
        MockServletContext servletContext = new MockServletContext();
        WebApplicationContext webappContext = new GenericWebApplicationContext(servletContext);
        ReflectionTestUtils.setField(searchService, "applicationContext", webappContext, WebApplicationContext.class);
        System.setProperty("c9.cluster.name", "c9.test.cluster");
        System.setProperty("c9.node.name", "c9.test.node");
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
    public void testIndexStatus() {
        // TODO add more tests once we have more index operations such as create and delete
        Map<String, IndexStatus> indexStatus = searchService.getIndexStatus("doesnotexistindex");
        Assert.assertNull(indexStatus);
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

}

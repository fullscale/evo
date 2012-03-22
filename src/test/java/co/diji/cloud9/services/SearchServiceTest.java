package co.diji.cloud9.services;

import junit.framework.Assert;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
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
        ClusterHealthResponse health = searchService.health();
        Assert.assertNotNull(health);
        Assert.assertEquals("c9.test.cluster", health.getClusterName());
        Assert.assertEquals(1, health.getNumberOfNodes());
    }

}

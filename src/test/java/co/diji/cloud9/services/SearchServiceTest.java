package co.diji.cloud9.services;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.exceptions.application.ApplicationExistsException;
import co.diji.cloud9.exceptions.application.InvalidApplicationNameException;
import co.diji.cloud9.exceptions.index.IndexCreationException;
import co.diji.cloud9.exceptions.index.IndexExistsException;
import co.diji.cloud9.exceptions.index.IndexMissingException;
import co.diji.cloud9.exceptions.type.TypeCreationException;
import co.diji.cloud9.exceptions.type.TypeExistsException;

public class SearchServiceTest {

    private static SearchService searchService;
    private static ConfigService config;

    @BeforeClass
    public static void setup() throws Cloud9Exception {
        System.setProperty("evo.cluster.name", "evo.test.cluster");
        System.setProperty("evo.node.name", "evo.test.node");

        searchService = new SearchService();
        config = ConfigService.getConfigService();
        ReflectionTestUtils.setField(searchService, "config", config, ConfigService.class);
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
        assertEquals("evo.test.cluster", health.getClusterName());
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
        assertEquals(2, indexStatus.size());
    }

    @Test
    public void testNodeInfo() {
        Map<String, NodeInfo> info = searchService.getNodeInfo();
        assertNotNull(info);
        assertEquals(1, info.size());
        assertEquals("evo.test.node", info.values().iterator().next().getNode().name());
    }

    @Test
    public void testNodeStats() {
        Map<String, NodeStats> stats = searchService.getNodeStats();
        assertNotNull(stats);
        assertEquals(1, stats.size());
        assertEquals("evo.test.node", stats.values().iterator().next().getNode().name());
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
        // we just need to test that we cant create system or app index
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
            searchService.createCollectionIndex("app");
            fail();
        } catch (IndexCreationException e) {
        }
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
    public void testGetTotalCollectionDocCount() {
        // TODO add more tests once we can add/remove docs
        long cnt = searchService.getTotalCollectionDocCount();
        assertEquals(0, cnt);
    }

    @Test
    public void testCreateApp() throws Exception {
    	
        assertFalse(searchService.hasApp("appwithdocs"));
        searchService.createApp("appwithdocs");
        assertTrue(searchService.hasApp("appwithdocs"));

        try {
            searchService.createApp("testapp");
        } catch (ApplicationExistsException e) {}
        assertTrue(searchService.hasApp("testapp"));

        try {
            searchService.createApp("css");
            fail();
        } catch (InvalidApplicationNameException e) {}
        assertFalse(searchService.hasApp("css"));

        try {
            searchService.createApp("js");
            fail();
        } catch (InvalidApplicationNameException e) {}
        assertFalse(searchService.hasApp("js"));

        try {
            searchService.createApp("img");
            fail();
        } catch (InvalidApplicationNameException e) {}
        assertFalse(searchService.hasApp("img"));

        searchService.createApp("anotherapp");
        assertTrue(searchService.hasApp("anotherapp"));
        assertFalse(searchService.hasIndex("anotherapp.app.app"));
    }
    
    @Test
    public void testGetAppNames() throws Exception {
    	assertTrue(searchService.hasApp("testapp"));
    	assertTrue(searchService.hasApp("appwithdocs"));
    	assertTrue(searchService.hasApp("anotherapp"));
    	
    	List<String> apps = new ArrayList<String>();
    	apps.add("anotherapp");
    	apps.add("appwithdocs");
    	apps.add("testapp");
    	
    	// app names are returned in sorted order
    	assertEquals(apps, searchService.getAppNames());
    }

    @Test
    public void testDeleteIndex() throws Exception {
        assertTrue(searchService.hasIndex("exists"));
        searchService.deleteIndex("exists");
        searchService.refreshIndex();
        assertFalse(searchService.hasIndex("exists"));
        assertTrue(searchService.hasIndex("oneshardnoreplicas"));
        assertTrue(searchService.hasIndex("oneshardonereplicas"));
        searchService.deleteIndex("oneshardnoreplicas", "oneshardonereplicas");
        searchService.refreshIndex();
        assertFalse(searchService.hasIndex("oneshardnoreplicas"));
        assertFalse(searchService.hasIndex("oneshardonereplicas"));

        // no exception should be thrown
        searchService.deleteIndex("some.junk.index.does.not.exist");
    }

    @Test
    public void testDeleteApp() throws Exception {
        assertTrue(searchService.hasApp("testapp"));
        searchService.deleteApp("testapp");
        //searchService.refreshApp();
        assertFalse(searchService.hasApp("testapp"));
        //assertTrue(searchService.hasApp("testapp2"));
        //assertTrue(searchService.hasApp("anotherapp"));
        searchService.deleteApp("testapp2", "anotherapp");
        //searchService.refreshApp();
        assertFalse(searchService.hasApp("testapp2"));
        assertFalse(searchService.hasApp("anotherapp"));

        // no exception should be thrown
        //searchService.deleteApp("some.junk.app.does.not.exist");
    }

    @Test
    public void testGetMappings() {
        Map<String, MappingMetaData> mappings = null;
        mappings = searchService.getMappings("indexwithhtmlmapping");
        assertEquals(1, mappings.size());
        assertTrue(mappings.containsKey("html"));
        mappings = searchService.getMappings("indexwithcssmapping");
        assertEquals(2, mappings.size());
        assertTrue(mappings.containsKey("html"));
        assertTrue(mappings.containsKey("css"));
        mappings = searchService.getTypes("doesnotexistindex");
        assertNull(mappings);
    }

    @Test
    public void testGetMapping() {
        MappingMetaData mapping = null;
        mapping = searchService.getMapping("indexwithhtmlmapping", "html");
        assertNotNull(mapping);
        mapping = searchService.getType("indexwithcssmapping", "html");
        assertNotNull(mapping);
        mapping = searchService.getMapping("indexwithcssmapping", "css");
        assertNotNull(mapping);
        mapping = searchService.getType("indexwithcssmapping", "junkmapping");
        assertNull(mapping);
    }

    @Test
    public void testPutMapping() throws Exception {
        Map<String, MappingMetaData> mappings = null;
        String testMapping = "{\"testmapping\":{\"properties\":{\"test\":{\"type\":\"string\",\"index\":\"no\",\"store\":\"yes\"}}}}";
        assertTrue(searchService.hasIndex("indexwithhtmlmapping"));
        mappings = searchService.getMappings("indexwithhtmlmapping");
        assertEquals(1, mappings.size());
        searchService.putMapping("indexwithhtmlmapping", "testmapping", testMapping);
        mappings = searchService.getMappings("indexwithhtmlmapping");
        assertEquals(2, mappings.size());
        assertTrue(mappings.containsKey("testmapping"));
        assertEquals(mappings.get("testmapping").source().string(), testMapping);
        assertFalse(searchService.hasIndex("indexwithtestmapping"));
        searchService.putMapping("indexwithtestmapping", "testmapping", testMapping, true);
        searchService.refreshIndex("indexwithtestmapping");
        mappings = searchService.getMappings("indexwithtestmapping");
        assertTrue(searchService.hasIndex("indexwithtestmapping"));
        assertEquals(1, mappings.size());
        assertTrue(mappings.containsKey("testmapping"));
        assertEquals(mappings.get("testmapping").source().string(), testMapping);
    }

    @Test
    public void testDeleteMapping() throws Exception {
        Map<String, MappingMetaData> mappings = null;
        assertTrue(searchService.deleteMapping("indexwithtestmapping", "testmapping"));
        searchService.refreshIndex();
        Thread.sleep(500);
        mappings = searchService.getMappings("indexwithtestmapping");
        assertNotNull(mappings);
        assertEquals(false, mappings.containsKey("testmapping"));
        assertEquals(0, mappings.size());
        assertTrue(searchService.deleteType("indexwithhtmlmapping", "testmapping"));
        searchService.refreshIndex();
        Thread.sleep(500);
        mappings = searchService.getMappings("indexwithhtmlmapping");
        assertNotNull(mappings);
        assertEquals(false, mappings.containsKey("testmapping"));
        assertEquals(1, mappings.size());
        assertEquals(false, searchService.deleteMapping("doesnotexistindex", "badmappingtype"));
        assertEquals(false, searchService.deleteType("indexwithhtmlmapping", "badmappingtype"));
    }

    @Test
    public void testCreateType() throws Exception {
        try {
            searchService.createType("indexwithhtmlmapping", "junk#W#$name");
            fail();
        } catch (TypeCreationException e) {
        }

        try {
            searchService.createType("doesnotexistindex", "dummytype");
            fail();
        } catch (IndexMissingException e) {
        }

        try {
            searchService.createType("indexwithhtmlmapping", "html");
            fail();
        } catch (TypeExistsException e) {
        }

        Map<String, MappingMetaData> mappings = searchService.getMappings("indexwithhtmlmapping");
        assertFalse(mappings.containsKey("mynewtype"));
        searchService.createType("indexwithhtmlmapping", "mynewtype");
        searchService.refreshIndex("indexwithhtmlmapping");
        mappings = searchService.getMappings("indexwithhtmlmapping");
        assertTrue(mappings.containsKey("mynewtype"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testImportApp() throws Exception {
        InputStream testzip = getClass().getClassLoader().getResourceAsStream("testappimport.zip");
        assertNotNull(testzip);
        assertEquals(false, searchService.hasApp("testappimport"));
        assertEquals(false, searchService.hasIndex("testappimport"));
        searchService.importApp("testappimport", testzip, false);
        searchService.refreshIndex();
        Thread.sleep(500);
        assertTrue(searchService.hasApp("testappimport"));
        assertTrue(searchService.hasIndex("testappimport"));
        MappingMetaData testAppMeta = searchService.getType("testappimport", "testapptype");
        assertNotNull(testAppMeta);
        Map<String, Object> testAppFields = (Map<String, Object>) testAppMeta.getSourceAsMap().get("properties");
        assertNotNull(testAppFields);
        assertEquals(4, testAppFields.size());
        assertTrue(testAppFields.containsKey("txtfield"));
        assertTrue(testAppFields.containsKey("intfield"));
        assertTrue(testAppFields.containsKey("datefield"));
        assertTrue(testAppFields.containsKey("strfield"));
        testzip = getClass().getClassLoader().getResourceAsStream("testappimport.zip");
        try {
            searchService.importApp("testappimport", null, false);
            fail();
        } catch (Cloud9Exception e) {
        }

        try {
            searchService.importApp("testappimport", testzip, false);
            fail();
        } catch (Cloud9Exception e) {
        }

        searchService.importApp("testappimport", testzip, true);
        searchService.refreshIndex();
        Thread.sleep(500);
        assertTrue(searchService.hasApp("testappimport"));
        assertTrue(searchService.hasIndex("testappimport"));
        testAppMeta = searchService.getType("testappimport", "testapptype");
        assertNotNull(testAppMeta);
        testAppFields = (Map<String, Object>) testAppMeta.getSourceAsMap().get("properties");
        assertNotNull(testAppFields);
        assertEquals(4, testAppFields.size());
        assertTrue(testAppFields.containsKey("txtfield"));
        assertTrue(testAppFields.containsKey("intfield"));
        assertTrue(testAppFields.containsKey("datefield"));
        assertTrue(testAppFields.containsKey("strfield"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExportApp() throws Exception {
        // test by exporting an app we know exists to a byte array, deleteing the app and
        // exported collections, then importing from the byte array and seeing if everything was created
        // correctly
        Map<String, String[]> exportCollections = new HashMap<String, String[]>();
        ByteArrayOutputStream exportedZipOut = new ByteArrayOutputStream();
        exportCollections.put("testappimport", new String[]{"testapptype"});
        searchService.exportApp("testappimport", exportedZipOut, exportCollections);
        searchService.deleteApp("testappimport");
        searchService.deleteIndex("testappimport");
        searchService.refreshIndex();
        Thread.sleep(500);
        assertEquals(false, searchService.hasApp("testappimport"));
        assertEquals(false, searchService.hasIndex("testappimport"));
        searchService.importApp("testappimport", new ByteArrayInputStream(exportedZipOut.toByteArray()), false);
        searchService.refreshIndex();
        Thread.sleep(500);
        assertTrue(searchService.hasApp("testappimport"));
        assertTrue(searchService.hasIndex("testappimport"));
        MappingMetaData testAppMeta = searchService.getType("testappimport", "testapptype");
        assertNotNull(testAppMeta);
        Map<String, Object> testAppFields = (Map<String, Object>) testAppMeta.getSourceAsMap().get("properties");
        assertNotNull(testAppFields);
        assertEquals(4, testAppFields.size());
        assertTrue(testAppFields.containsKey("txtfield"));
        assertTrue(testAppFields.containsKey("intfield"));
        assertTrue(testAppFields.containsKey("datefield"));
        assertTrue(testAppFields.containsKey("strfield"));

        try {
            searchService.exportApp("testappimport", null, exportCollections);
            fail();
        } catch (Cloud9Exception e) {
        }

        try {
            searchService.exportApp("does.not.exist", exportedZipOut, exportCollections);
            fail();
        } catch (Cloud9Exception e) {
        }

        // test that all types are exported when type array is null
        // should be same type as above
        exportedZipOut = new ByteArrayOutputStream();
        exportCollections.put("testappimport", null);
        searchService.exportApp("testappimport", exportedZipOut, exportCollections);
        searchService.deleteApp("testappimport");
        searchService.deleteIndex("testappimport");
        searchService.refreshIndex();
        Thread.sleep(500);
        assertEquals(false, searchService.hasApp("testappimport"));
        assertEquals(false, searchService.hasIndex("testappimport"));
        searchService.importApp("testappimport", new ByteArrayInputStream(exportedZipOut.toByteArray()), false);
        searchService.refreshIndex();
        Thread.sleep(500);
        assertTrue(searchService.hasApp("testappimport"));
        assertTrue(searchService.hasIndex("testappimport"));
        testAppMeta = searchService.getType("testappimport", "testapptype");
        assertNotNull(testAppMeta);
        testAppFields = (Map<String, Object>) testAppMeta.getSourceAsMap().get("properties");
        assertNotNull(testAppFields);
        assertEquals(4, testAppFields.size());
        assertTrue(testAppFields.containsKey("txtfield"));
        assertTrue(testAppFields.containsKey("intfield"));
        assertTrue(testAppFields.containsKey("datefield"));
        assertTrue(testAppFields.containsKey("strfield"));

        // test no mapping export
        exportedZipOut = new ByteArrayOutputStream();
        searchService.exportApp("testappimport", exportedZipOut, null);
        searchService.deleteApp("testappimport");
        searchService.deleteIndex("testappimport");
        searchService.refreshIndex();
        Thread.sleep(500);
        assertEquals(false, searchService.hasApp("testappimport"));
        assertEquals(false, searchService.hasIndex("testappimport"));
        searchService.importApp("testappimport", new ByteArrayInputStream(exportedZipOut.toByteArray()), false);
        searchService.refreshIndex();
        Thread.sleep(500);
        assertTrue(searchService.hasApp("testappimport"));
        assertEquals(false, searchService.hasIndex("testappimport"));
    }
}

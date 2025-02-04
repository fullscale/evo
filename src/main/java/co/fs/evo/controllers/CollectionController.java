package co.fs.evo.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import co.fs.evo.exceptions.index.IndexException;
import co.fs.evo.services.SearchService;

@Controller
@RequestMapping("/evo/content")
public class CollectionController {

    private static final XLogger logger = XLoggerFactory.getXLogger(CollectionController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> get(@PathVariable String collection) {
        logger.entry(collection);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("collection", collection);

        Map<String, MappingMetaData> types = searchService.getTypes(collection);
        logger.debug("types: {}", types);
        if (types != null) {
            Map<String, Object> typesResp = new HashMap<String, Object>();
            for (Entry<String, MappingMetaData> type : types.entrySet()) {
                logger.debug("type: {}", type);
                try {
                    typesResp.put(type.getKey(), type.getValue().sourceAsMap().get("properties"));
                } catch (IOException e) {
                    logger.debug("Error getting type properties", e);
                }
            }

            resp.put("types", typesResp);
        }

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> create(@PathVariable String collection) {
        logger.entry(collection);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            searchService.createIndex(collection);
            logger.info("Successfully created collection: {}", collection);
            resp.put("status", "ok");
        } catch (IndexException e) {
            logger.warn("Error creating collection: {}", e.getMessage());
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.PUT)
    public void update(@PathVariable String collection) {
        logger.entry(collection);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> delete(@PathVariable String collection) {
        logger.entry(collection);
        Map<String, Object> resp = new HashMap<String, Object>();
        searchService.deleteIndex(collection);
        resp.put("status", "ok");
        
        logger.exit();
        return resp;
    }

}

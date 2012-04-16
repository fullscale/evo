package co.diji.cloud9.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import co.diji.cloud9.exceptions.index.IndexException;
import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/cloud9/content")
public class CollectionController {

    private static final Logger logger = LoggerFactory.getLogger(CollectionController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> get(@PathVariable String collection) {
        logger.trace("in controller=collection action=get collection: ", collection);
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

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> create(@PathVariable String collection) {
        logger.trace("in controller=collection action=create collection: {}", collection);
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

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.PUT)
    public void update(@PathVariable String collection) {
        logger.trace("in controller=collection action=update collection: {}", collection);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> delete(@PathVariable String collection) {
        logger.trace("in controller=collection action=delete collection: {}", collection);
        Map<String, Object> resp = new HashMap<String, Object>();
        searchService.deleteIndex(collection);
        resp.put("status", "ok");
        return resp;
    }

}

package co.diji.cloud9.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/cloud9/content")
public class TypeController {

    private static final Logger logger = LoggerFactory.getLogger(TypeController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> get(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=get collection: {} type:{}", collection, type);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("collection", collection);
        resp.put("type", type);

        MappingMetaData typeInfo = searchService.getType(collection, type);
        if (typeInfo != null) {
            try {
                resp.put("fields", typeInfo.sourceAsMap().get("properties"));
            } catch (IOException e) {
                logger.debug("Error getting type properties", e);
            }
        }

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> create(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=create collection:{} type:{}", collection, type);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            searchService.createType(collection, type);
            logger.info("Successfully created type: {}", type);
            resp.put("status", "ok");
        } catch (Cloud9Exception e) {
            logger.warn("Error creating type: {}, {}", type, e.getMessage());
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.PUT)
    public void update(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=update collection:{} type:{}", collection, type);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=delete collection:{} type:{}", collection, type);
    }

}

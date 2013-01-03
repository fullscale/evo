package co.fs.evo.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.json.simple.JSONObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import co.fs.evo.exceptions.Cloud9Exception;
import co.fs.evo.services.SearchService;

@Controller
@RequestMapping("/evo/content")
public class TypeController {

    private static final XLogger logger = XLoggerFactory.getXLogger(TypeController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> get(@PathVariable String collection, @PathVariable String type) {
        logger.entry(collection, type);
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

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> create(@PathVariable String collection, @PathVariable String type) {
        logger.entry(collection, type);
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

        logger.exit();
        return resp;
    }

    @ResponseBody
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public Map<String, Object> update(@PathVariable String collection, @PathVariable String type,
            @RequestBody Map<String, Object> mappings) {
        logger.entry(collection, type);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            MappingMetaData typeInfo = searchService.getType(collection, type);
            logger.debug("typeInfo: {}", typeInfo);
            if (typeInfo == null) {
                throw new Cloud9Exception("Unable to get type info for: " + type);
            }

            Map<String, Object> currentMappings = typeInfo.sourceAsMap();
            Map<String, Object> currentFields = (Map<String, Object>) currentMappings.get("properties");
            logger.debug("currentMappings: {}", currentMappings);
            logger.debug("currentFields: {}", currentFields);
            for (Entry<String, Object> mapping : mappings.entrySet()) {
                String fieldName = mapping.getKey();
                logger.debug("fieldName: {}", fieldName);
                if (currentFields.containsKey(fieldName)) {
                    throw new Cloud9Exception("Field already exists: " + fieldName);
                }

                currentFields.put(fieldName, mapping.getValue());
            }

            JSONObject newMapping = new JSONObject();
            newMapping.put(type, currentMappings);
            logger.debug("newMapping: {}", newMapping);
            searchService.putMapping(collection, type, newMapping.toJSONString());

            resp.put("status", "ok");
        } catch (Exception e) {
            logger.warn("Error updating mappings: {}", e.getMessage());
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> delete(@PathVariable String collection, @PathVariable String type) {
        logger.entry(collection, type);
        Map<String, Object> resp = new HashMap<String, Object>();
        searchService.deleteType(collection, type);
        resp.put("status", "ok");
        logger.exit();
        return resp;
    }

}

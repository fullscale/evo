package co.diji.cloud9.controllers;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/evo/content")
public class DocumentController {

    private static final XLogger logger = XLoggerFactory.getXLogger(DocumentController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> get(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.entry(collection, type, id);
        Map<String, Object> resp = new HashMap<String, Object>();
        
        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Map<String, Object> create(@PathVariable String collection, @PathVariable String type, @PathVariable String id,
            @RequestBody Map<String, Object> source) {
        logger.entry(collection, type, id);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            IndexResponse idxResp = searchService.indexDoc(collection, type, id, source);
            logger.debug("idxResp: {}", idxResp);
            if (idxResp == null) {
                throw new Cloud9Exception("Error indexing document: " + id);
            }

            resp.put("status", "ok");
            resp.put("id", idxResp.id());
            resp.put("version", idxResp.version());
        } catch (Cloud9Exception e) {
            logger.warn(e.getMessage());
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.PUT, produces = "application/json")
    public Map<String, Object> update(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.entry(collection, type, id);
        Map<String, Object> resp = new HashMap<String, Object>();
        
        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> delete(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.entry(collection, type, id);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            DeleteResponse delResp = searchService.deleteDoc(collection, type, id);
            logger.debug("delResp: {}", delResp);
            if (delResp == null) {
                logger.debug("delResp is null");
                throw new Cloud9Exception("Error deleteing document: " + id);
            }

            resp.put("status", "ok");
            resp.put("id", delResp.id());
            resp.put("version", delResp.version());
        } catch (Cloud9Exception e) {
            logger.warn(e.getMessage());
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        logger.exit();
        return resp;
    }

}

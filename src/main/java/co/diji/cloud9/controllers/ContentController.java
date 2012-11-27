package co.diji.cloud9.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.json.simple.JSONObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/evo/content")
public class ContentController extends BaseController {

    private static final XLogger logger = XLoggerFactory.getXLogger(ContentController.class);

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/publish", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> add(@PathVariable String collection, @PathVariable String type) {
        logger.entry(collection, type);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("collection", collection);
        resp.put("type", type);
        resp.put("id", UUID.randomUUID().toString());

        MappingMetaData typeInfo = searchService.getType(collection, type);
        logger.debug("typeInfo: {}", typeInfo);
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
    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView get(ModelMap model) {
        logger.entry();

        JSONObject indices = new JSONObject();
        Map<String, IndexStatus> collectionStatus = (Map<String, IndexStatus>) model.get("status");

        for (String indexItem : collectionStatus.keySet()) {
            logger.debug("processing collection: {}", indexItem);
            IndexStatus indexStatus = collectionStatus.get(indexItem);

            JSONObject stats = new JSONObject();
            stats.put("docs", String.valueOf(indexStatus.docs().numDocs()));
            stats.put("size", indexStatus.getPrimaryStoreSize().toString());
            indices.put(indexItem, stats);
        }

        model.addAttribute("indices", indices.toString());

        logger.exit();
        return new ModelAndView("collections", model);
    }

}

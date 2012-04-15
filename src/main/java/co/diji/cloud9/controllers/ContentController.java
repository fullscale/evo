package co.diji.cloud9.controllers;

import java.util.Map;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import co.diji.cloud9.services.ConfigService;
import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/cloud9/content")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/publish", method = RequestMethod.GET)
    public void add(@PathVariable String collection, @PathVariable String type) {
        logger.info("content add collection:" + collection + " type:" + type);
    }

    @ResponseBody
    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView get() {
        logger.trace("enter controller=content action=get");

        ClusterHealthResponse clusterHealth = searchService.getClusterHealth();
        long count = searchService.getTotalCollectionDocCount();
        Map<String, IndexStatus> collectionStatus = searchService.getCollectionStatus();
        Map<String, NodeInfo> nodeInfo = searchService.getNodeInfo();
        Map<String, NodeStats> nodeStats = searchService.getNodeStats();

        JSONObject indices = new JSONObject();

        for (String indexItem : collectionStatus.keySet()) {
            IndexStatus indexStatus = collectionStatus.get(indexItem);

            JSONObject stats = new JSONObject();
            stats.put("docs", String.valueOf(indexStatus.docs().numDocs()));
            stats.put("size", indexStatus.getPrimaryStoreSize().toString());
            indices.put(indexItem, stats);
        }

        ModelAndView mav = new ModelAndView();

        mav.addObject("cluster", clusterHealth);
        mav.addObject("stats", nodeStats);
        mav.addObject("nodes", nodeInfo);
        mav.addObject("status", collectionStatus);
        mav.addObject("count", count);
        mav.addObject("indices", indices.toString());
        mav.addObject("build", config.get("build"));

        mav.setViewName("collections");
        
        logger.trace("exit get: {}", mav);
        return mav;
    }

}

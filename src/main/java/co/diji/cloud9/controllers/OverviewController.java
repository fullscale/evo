package co.diji.cloud9.controllers;

import java.util.Map;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import co.diji.cloud9.services.SearchService;

@Controller
public class OverviewController {

    private static final Logger logger = LoggerFactory.getLogger(OverviewController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = {"/", "/cloud9", "/cloud9/overview"}, method = RequestMethod.GET)
    public ModelAndView get() {
    	logger.trace("enter controller=overview action=get");
    	
    	ClusterHealthResponse clusterHealth = searchService.getClusterHealth();
    	long count = searchService.getTotalCollectionDocCount();
    	Map<String, IndexStatus> collectionStatus = searchService.getCollectionStatus();
    	Map<String, NodeInfo> nodeInfo = searchService.getNodeInfo();
    	Map<String, NodeStats> nodeStats = searchService.getNodeStats();

        ModelAndView mav = new ModelAndView();

        mav.addObject("cluster", clusterHealth);
        mav.addObject("stats", nodeStats);
        mav.addObject("nodes", nodeInfo);
        mav.addObject("status", collectionStatus);
        mav.addObject("count", count);
        //mav.addObject("build", "build" + app.build);
        
        mav.setViewName("overview");
        return mav;
    }

}

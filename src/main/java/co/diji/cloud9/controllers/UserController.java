package co.diji.cloud9.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.ClusterState;

import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/cloud9/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    public void get(@PathVariable String userId) {
        logger.info("user get userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.POST)
    public void create(@PathVariable String userId) {
        logger.info("user create userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    public void update(@PathVariable String userId) {
        logger.info("user update userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String userId) {
        logger.info("user delete userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView list() {

        ClusterHealthResponse clusterHealth = searchService.getClusterHealth();
        //Map<String, Integer> clusterStatus = searchService.getClusterStatus();
        Map<String, NodeInfo> nodeInfo = searchService.getNodeInfo();
        Map<String, NodeStats> nodeStats = searchService.getNodeStats();

        ModelAndView mav = new ModelAndView();
        mav.setViewName("users");
        mav.addObject("cluster", clusterHealth);
        mav.addObject("stats", nodeStats);
        mav.addObject("nodes", nodeInfo);
        //mav.addObject("status", clusterStatus);
        //mav.addObject("build", "build" + app.build);
        return mav;
    }

}

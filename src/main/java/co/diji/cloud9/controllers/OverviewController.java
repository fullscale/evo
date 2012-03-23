package co.diji.cloud9.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;

import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/cloud9/overview")
public class OverviewController {

    private static final Logger logger = LoggerFactory.getLogger(OverviewController.class);

    @Autowired
    protected SearchService searchService;

/*
    def index = { 
      
        def indices = searchService.indices();
        def nodeInfo = searchService.nodeInfo();
        def stats = searchService.nodeStats();

        def nodes = [:]
        nodeInfo.nodes.each() {
            nodes[it.node.nodeId] = [
            name:it.node.nodeName,
            host:it.node.address.address.getAddress().getHostAddress()];
        }
        render(view:"overview", model:[
            stats:stats,
            nodes:nodes,
            cluster:searchService.health(),
            status:searchService.clusterStatus(),
            build:"build " + ApplicationHolder.application.metadata['app.build']
        ]);
*/

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView get() {
        //logger.info("overview get");

    	//searchService.indices();
    	//Map<String, NodeInfo> nodeinfo = searchService.getNodeInfo();
    	Map<String, NodeStats> nodestats = searchService.getNodeStats();

        ModelAndView mav = new ModelAndView();
        mav.setViewName("overview");
        mav.addObject("stats", nodestats);
        return mav;
    }

}

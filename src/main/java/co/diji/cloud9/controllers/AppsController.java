package co.diji.cloud9.controllers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
public class AppsController {

    private static final Logger logger = LoggerFactory.getLogger(AppsController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;

    @ResponseBody
    @RequestMapping(value = "/cloud9/ide/{app}", method = RequestMethod.GET)
    public ModelAndView showIde(@PathVariable String app) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("editor");
        mav.addObject("app", app);
        return mav;
    }

    @ResponseBody
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/cloud9/apps", method = RequestMethod.GET)
    public ModelAndView list() {
        logger.trace("enter controller=apps action=list");

        ClusterHealthResponse clusterHealth = searchService.getClusterHealth();
        long count = searchService.getTotalCollectionDocCount();
        Map<String, IndexStatus> collectionStatus = searchService.getCollectionStatus();
        Map<String, IndexStatus> apps = searchService.getAppStatus();
        Map<String, NodeInfo> nodeInfo = searchService.getNodeInfo();
        Map<String, NodeStats> nodeStats = searchService.getNodeStats();

        JSONObject appResp = new JSONObject();

        for (String app : apps.keySet()) {
            IndexStatus appStatus = apps.get(app);

            JSONObject stats = new JSONObject();
            stats.put("docs", String.valueOf(appStatus.docs().numDocs()));
            stats.put("size", appStatus.getPrimaryStoreSize().toString());
            appResp.put(app, stats);
        }

        ModelAndView mav = new ModelAndView();

        mav.addObject("cluster", clusterHealth);
        mav.addObject("stats", nodeStats);
        mav.addObject("nodes", nodeInfo);
        mav.addObject("status", collectionStatus);
        mav.addObject("count", count);
        mav.addObject("apps", appResp.toString());
        mav.addObject("build", config.get("build"));

        mav.setViewName("applications");
        return mav;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.GET)
    public void listContentTypes(@PathVariable String app) {
        logger.info("apps listContentTypes app:" + app);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.POST)
    public void createApp(@PathVariable String app) {
        logger.info("apps createApp app:" + app);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.DELETE)
    public void deleteApp(@PathVariable String app) {
        logger.info("apps deleteApp app:" + app);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}", method = RequestMethod.GET)
    public void listResources(@PathVariable String app, @PathVariable String dir) {
        logger.info("apps listResources app:" + app + " dir:" + dir);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.GET)
    public void getResourceFromDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps getResourceFromDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.POST)
    public void createResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps createResourceInDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.PUT)
    public void updateResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps updateResourceInDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.DELETE)
    public void deleteResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps deleteResourceInDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/_rename", method = RequestMethod.PUT)
    public void renameResource(@PathVariable String app, @PathVariable String dir) {
        logger.info("apps renameResource app:" + app + " dir:" + dir);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:!(css|images|js)}/{dir}/{resource}")
    public void processResource(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletRequest request) {
        logger.info(request.getMethod() + ": apps processResource app:" + app + " dir:" + dir + " resource:" + resource);
    }

}

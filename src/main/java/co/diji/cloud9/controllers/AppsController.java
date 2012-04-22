package co.diji.cloud9.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.services.ConfigService;
import co.diji.cloud9.services.SearchService;

@Controller
public class AppsController {

    private static final Logger logger = LoggerFactory.getLogger(AppsController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;

    /**
     * Validates that the resource has the correct extension, if not, it adds it.
     * 
     * @param resource the resource name to validate
     * @param type the type of the resource
     * @return the resource name with the correct extension
     */
    private String validateResource(String resource, String type) {
        logger.trace("in validateResource resource:{} type:{}", resource, type);
        if (!resource.endsWith(type)) {
            resource = resource + "." + type;
            logger.debug("resource with extension: {}", resource);
        }

        logger.trace("exit validateResource: {}", resource);
        return resource;
    }

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
        logger.trace("in controller=apps action=list");

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

        logger.trace("exit list: {}", mav);
        return mav;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.GET, produces = "application/json")
    public List<String> listContentTypes(@PathVariable String app) {
        logger.trace("in controller=apps action=listContentTypes app:{}", app);
        List<String> resp = new ArrayList<String>();

        Map<String, MappingMetaData> appTypes = searchService.getAppTypes(app);
        logger.debug("appTypes: {}", appTypes);
        if (appTypes != null) {
            for (String appType : appTypes.keySet()) {
                logger.debug("adding appType: {}", appType);
                resp.add(appType);
            }
        }

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> createApp(@PathVariable String app) {
        logger.trace("in controller=apps action=createApp app:{}", app);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            searchService.createApp(app);
            resp.put("status", "ok");
        } catch (IndexException e) {
            logger.warn(e.getMessage());
            logger.trace("exception: ", e);
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> deleteApp(@PathVariable String app) {
        logger.trace("in controller=apps action=deleteApp app:{}", app);
        Map<String, Object> resp = new HashMap<String, Object>();

        searchService.deleteApp(app);
        resp.put("status", "ok");

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}", method = RequestMethod.GET, produces = "application/json")
    public List<String> listResources(@PathVariable String app, @PathVariable String dir) {
        logger.trace("in controller=apps action=listResources app:{} dir:{}", app, dir);
        List<String> resp = new ArrayList<String>();
        String appIdx = searchService.appsWithSuffix(app)[0];
        logger.debug("appIdx: {}", appIdx);
        SearchResponse searchResp = searchService.matchAll(appIdx, dir, null);
        logger.debug("searchResp:{}", searchResp);
        if (searchResp != null) {
            for (SearchHit hit : searchResp.hits()) {
                resp.add(hit.getId());
            }
        }

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.GET)
    public void getResourceFromDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletResponse response) {
        logger.trace("in controller=apps action=getResourceFromDir app:{} dir:{} resource:{} response:{}", new Object[]{
                app, dir, resource, response});
        String appIdx = searchService.appsWithSuffix(app)[0];
        logger.debug("appIdx: {}", appIdx);

        GetResponse res = searchService.getDoc(appIdx, dir, resource, null);
        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();

            logger.debug("res: {}", res);
            if (res == null) {
                throw new Cloud9Exception("Unable to get resource");
            }

            Map<String, Object> source = res.sourceAsMap();
            logger.debug("source: {}", source);
            if (source == null) {
                throw new Cloud9Exception("Unable to get resource source");
            }

            String mime = (String) source.get("mime");
            logger.debug("mime: {}", mime);
            response.setContentType(mime);

            String code = (String) source.get("code");
            logger.debug("code: {}", code);
            if (mime.startsWith("image")) {
                logger.debug("decoding base64 data");
                byte[] data = Base64.decodeBase64(code);
                out.write(data);
            } else {
                out.print(code);
            }
        } catch (Exception e) {
            logger.debug("Error getting resource", e);
            response.setStatus(400);
        } finally {
            logger.debug("closing output stream");
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.debug("Error closing output stream", e);
                }
            }
        }
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> createResourceInDir(@PathVariable String app, @PathVariable String dir,
            @PathVariable String resource, @RequestBody String data) {
        logger.trace("in controller=apps action=createResourceInDir app:{} dir:{} resource:{} data:{}", new Object[]{
                app, dir, resource, data});
        Map<String, Object> resp = new HashMap<String, Object>();
        String mime = "text/plain";

        try {
            if ("html".equals(dir)) {
                mime = "text/html";
                resource = validateResource(resource, dir);
            } else if ("css".equals(dir)) {
                mime = "text/css";
                resource = validateResource(resource, dir);
            } else if ("js".equals(dir)) {
                mime = "application/javascript";
                resource = validateResource(resource, dir);
            } else if ("images".equals(dir)) {
                int sIdx = resource.indexOf('.');
                logger.debug("sIdx: {}", sIdx);
                if (sIdx == -1) {
                    logger.warn("Image without extension: {}", resource);
                    throw new Cloud9Exception("Image without extension: " + resource);
                }
                String suffix = resource.substring(sIdx + 1, resource.length());
                logger.debug("suffix: {}", suffix);
                if ("jpg".equals(suffix)) {
                    suffix = "jpeg";
                    logger.debug("new suffix: {}", suffix);
                }

                mime = "image/" + suffix;
            } else if ("controllers".equals(dir)) {
                mime = "application/javascript";
            }

            IndexResponse indexResponse = searchService.indexAppDoc(app, dir, resource, data, mime);
            resp.put("status", "ok");
            resp.put("id", indexResponse.id());
            resp.put("version", indexResponse.version());
        } catch (Cloud9Exception e) {
            logger.debug("Error creating resource", e);
            resp.put("status", "failed");
            resp.put("response", e.getMessage());
        }

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public Map<String, Object> updateResourceInDir(@PathVariable String app, @PathVariable String dir,
            @PathVariable String resource, @RequestBody Map<String, Object> data) {
        logger.trace("in controller=apps action=updateResourceInDir app:{} dir:{} resource:{}", new Object[]{app, dir, resource});
        Map<String, Object> resp = new HashMap<String, Object>();
        String appIdx = searchService.appsWithSuffix(app)[0];
        logger.debug("addIdx: {}", appIdx);

        IndexResponse indexResponse = searchService.indexDoc(appIdx, dir, resource, data);
        resp.put("status", "ok");
        resp.put("id", indexResponse.id());
        resp.put("version", indexResponse.version());

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> deleteResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.trace("in controller=apps action=deleteResourceInDir app:{} dir:{} resource:{}", new Object[]{app, dir, resource});
        Map<String, Object> resp = new HashMap<String, Object>();
        String appIdx = searchService.appsWithSuffix(app)[0];
        logger.debug("appIdx: {}", appIdx);

        DeleteResponse deleteResponse = searchService.deleteDoc(appIdx, dir, resource);
        resp.put("status", "ok");
        resp.put("id", deleteResponse.id());
        resp.put("version", deleteResponse.version());

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/_rename", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public Map<String, Object> renameResource(@PathVariable String app, @PathVariable String dir,
            @RequestBody Map<String, Object> data) {
        logger.trace("in controller=apps action=renameResource app:{} dir:{} data:{}", new Object[]{app, dir, data});
        Map<String, Object> resp = new HashMap<String, Object>();
        String appIdx = searchService.appsWithSuffix(app)[0];
        logger.debug("appIdx: {}", appIdx);

        try {
            String oldId = (String) resp.get("from");
            String newId = (String) resp.get("to");
            if (oldId == null || newId == null) {
                throw new Cloud9Exception("Must specify the old and new ids");
            }

            GetResponse oldDoc = searchService.getDoc(appIdx, dir, oldId, null);
            if (oldDoc == null) {
                throw new Cloud9Exception("Resource does not exist");
            }

            IndexResponse indexResponse = searchService.indexDoc(appIdx, dir, newId, oldDoc.sourceAsMap());
            if (indexResponse.id().equals(newId)) {
                searchService.deleteDoc(appIdx, dir, oldId);
                resp.put("status", "ok");
                resp.put("id", indexResponse.id());
                resp.put("version", indexResponse.version());
            } else {
                resp.put("status", "failed");
                resp.put("response", "unable to rename resource");
            }
        } catch (Cloud9Exception e) {
            logger.error("Error renaming resource: {}", e.getMessage());
            logger.debug("exception", e);
            resp.put("status", "failed");
            resp.put("response", e.getMessage());
        }

        return resp;
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

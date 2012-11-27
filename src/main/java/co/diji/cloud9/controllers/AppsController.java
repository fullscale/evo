package co.diji.cloud9.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import co.diji.cloud9.apps.resources.ResourceHelper;
import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.exceptions.application.ApplicationExistsException;
import co.diji.cloud9.exceptions.application.InvalidApplicationNameException;
import co.diji.cloud9.exceptions.mapping.MappingException;

@Controller
public class AppsController extends BaseController {

    private static final XLogger logger = XLoggerFactory.getXLogger(AppsController.class);

    @Autowired
    private ResourceHelper resourceHelper;
    
    /**
     * Validates that the resource has the correct extension, if not, it adds it.
     * 
     * @param resource the resource name to validate
     * @param type the type of the resource
     * @return the resource name with the correct extension
     */
    private String validateResource(String resource, String type) {
        logger.entry(resource, type);
        if (!resource.endsWith(type)) {
            resource = resource + "." + type;
            logger.debug("resource with extension: {}", resource);
        }

        logger.exit(resource);
        return resource;
    }

    @ResponseBody
    @RequestMapping(value = "/evo/ide/{app}", method = RequestMethod.GET)
    public ModelAndView showIde(@PathVariable String app) {
        logger.entry(app);
        ModelAndView mav = new ModelAndView();
        mav.setViewName("editor");
        mav.addObject("app", app);
        logger.exit();
        return mav;
    }

    @ResponseBody
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/evo/apps", method = RequestMethod.GET)
    public ModelAndView list(ModelMap model) {
        logger.entry();

        List<String> apps = searchService.getAppNames();
        JSONObject appResp = new JSONObject();

        for (String app : apps) {
        	// TODO: returns a dummy object for UI which needs to be updated
            JSONObject stats = new JSONObject();
            stats.put("docs", "4");
            stats.put("size", "");
            appResp.put(app, stats);
        }

        model.addAttribute("apps", appResp.toString());

        logger.exit();
        return new ModelAndView("applications", model);
    }

    @ResponseBody
    @RequestMapping(value = "/evo/apps/{app}", method = RequestMethod.GET, produces = "application/json")
    public List<String> listContentTypes(@PathVariable String app) {
        logger.entry(app);
        List<String> response;
        try {
        	response = searchService.getAppTypes(app);
        } catch (InvalidApplicationNameException e) {
        	response = new ArrayList<String>();
        }
        logger.exit();
        return response;
    }

    @ResponseBody
    @RequestMapping(value = "/evo/apps/{app}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> createApp(@PathVariable String app) {
        logger.entry(app);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            searchService.createApp(app);
            resp.put("status", "ok");
        } catch (InvalidApplicationNameException e) {
            logger.warn(e.getMessage());
            logger.debug("exception: ", e);
            resp.put("status", "error");
            resp.put("response", "Inavlid application name");
        } catch (ApplicationExistsException e) {
            logger.warn(e.getMessage());
            logger.debug("exception: ", e);
            resp.put("status", "error");
            resp.put("response", "Application name already exists");
        } catch (MappingException e) {
            logger.warn(e.getMessage());
            logger.debug("exception: ", e);
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/evo/apps/{app}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> deleteApp(@PathVariable String app) {
        logger.entry(app);
        Map<String, Object> resp = new HashMap<String, Object>();
        
        // evict cached resources
        resourceHelper.evict(app);
        
        // delete the app
        searchService.deleteApp(app);
        resp.put("status", "ok");

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/evo/apps/{app}/{dir}", method = RequestMethod.GET, produces = "application/json")
    public List<String> listResources(@PathVariable String app, @PathVariable String dir) {
        logger.entry(app, dir);
        List<String> resp = new ArrayList<String>();
        
        SearchResponse searchResp = searchService.matchAll(searchService.APP_INDEX, app + "_" + dir, null);
        logger.debug("searchResp:{}", searchResp);
        if (searchResp != null) {
            for (SearchHit hit : searchResp.hits()) {
                resp.add(hit.getId());
            }
        }

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/evo/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.GET)
    public void getResourceFromDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletResponse response) {
        logger.entry(app, dir, resource);

        // TODO: maybe hide this in the searchService?
        GetResponse res = searchService.getDoc(searchService.APP_INDEX, app + "_" + dir, resource, null);
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
    @RequestMapping(value = "/evo/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> createResourceInDir(@PathVariable String app, @PathVariable String dir,
            @PathVariable String resource, @RequestBody String data) {
        logger.entry(app, dir, resource);
        Map<String, Object> resp = new HashMap<String, Object>();
        String mime = "text/plain";

        try {
            if ("html".equals(dir) || "partials".equals(dir)) {
                mime = "text/html";
                resource = validateResource(resource, "html");
            } else if ("css".equals(dir)) {
                mime = "text/css";
                resource = validateResource(resource, "css");
            } else if ("js".equals(dir) || "lib".equals(dir)) {
                mime = "application/javascript";
                resource = validateResource(resource, "js");
            } else if ("img".equals(dir)) {
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
            } else if ("server-side".equals(dir)) {
                mime = "application/javascript";
            }

            // nothing should be cached since this is new, but just to be safe, evict from cache
            resourceHelper.evict(app, dir, resource);
            
            // index the doc
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
    @RequestMapping(value = "/evo/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public Map<String, Object> updateResourceInDir(@PathVariable String app, @PathVariable String dir,
            @PathVariable String resource, @RequestBody Map<String, Object> data) {
        logger.entry(app, dir, resource);
        Map<String, Object> resp = new HashMap<String, Object>();
        logger.debug("addIdx: {}", app);

        // evict cached resource
        resourceHelper.evict(app, dir, resource);
        
        IndexResponse indexResponse = searchService.indexDoc(searchService.APP_INDEX, app+"_"+dir, resource, data);
        resp.put("status", "ok");
        resp.put("id", indexResponse.id());
        resp.put("version", indexResponse.version());

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/evo/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> deleteResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.entry(app, dir, resource);
        Map<String, Object> resp = new HashMap<String, Object>();
        logger.debug("appIdx: {}", app);

        // evict cached resource
        resourceHelper.evict(app, dir, resource);
        
        DeleteResponse deleteResponse = searchService.deleteDoc(searchService.APP_INDEX, app+"_"+dir, resource);
        resp.put("status", "ok");
        resp.put("id", deleteResponse.id());
        resp.put("version", deleteResponse.version());

        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/evo/apps/{app}/{dir}/_rename", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public Map<String, Object> renameResource(@PathVariable String app, @PathVariable String dir,
            @RequestBody Map<String, Object> data) {
        logger.entry(app, dir);
        Map<String, Object> resp = new HashMap<String, Object>();
        logger.debug("appIdx: {}", app);

        try {
            String oldId = (String) data.get("from");
            String newId = (String) data.get("to");
            if (oldId == null || newId == null) {
                throw new Cloud9Exception("Must specify the old and new ids");
            }

            GetResponse oldDoc = searchService.getDoc(searchService.APP_INDEX, app+"_"+dir, oldId, null);
            if (oldDoc == null) {
                throw new Cloud9Exception("Resource does not exist");
            }

            // evict from caches, even the new name just to be safe
            resourceHelper.evict(app, dir, newId);
            resourceHelper.evict(app, dir, oldId);
            
            // index the new doc
            IndexResponse indexResponse = searchService.indexDoc(searchService.APP_INDEX, app+"_"+dir, newId, oldDoc.sourceAsMap());
            if (indexResponse.id().equals(newId)) {
                // delete the old doc
                searchService.deleteDoc("app", app+"_"+dir, oldId);
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

        logger.exit();
        return resp;
    }

    // TODO: there are hacks for mars, remove once we fix authentication
    @ResponseBody
    @RequestMapping(value = "/v2/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public Map<String, Object> pushAppFile(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            @RequestBody Map<String, Object> data) {
        logger.entry(app, dir, resource);
        Map<String, Object> resp = new HashMap<String, Object>();
        logger.debug("addIdx: {}", app);

        try {
            searchService.createApp(app);
        } catch (InvalidApplicationNameException e) {
            logger.warn("Error pushing application file: {}", e.getMessage());
            resp.put("status", "error");
            resp.put("response", "Inavlid application name");
            return resp;
        } catch (ApplicationExistsException e) {
            logger.warn("Error pushing application file: {}", e.getMessage());
            resp.put("status", "error");
            resp.put("response", "Application name already exists");
            return resp;
        } catch (MappingException e) {
            logger.warn("Error pushing application file: {}", e.getMessage());
            resp.put("status", "error");
            resp.put("response", e.getMessage());
            return resp;
        }

        // evict old cached resource
        resourceHelper.evict(app, dir, resource);
        
        // index the new resource
        IndexResponse indexResponse = searchService.indexDoc(searchService.APP_INDEX, app+"_"+dir, resource, data);
        resp.put("status", "ok");
        resp.put("id", indexResponse.id());
        resp.put("version", indexResponse.version());

        logger.exit();
        return resp;
    }

    // TODO: there are hacks for mars, remove once we fix authentication
    @ResponseBody
    @RequestMapping(value = "/v2/apps/{app}/{dir}/{resource:.*}", method = RequestMethod.GET)
    public void pullAppFile(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletResponse response) {
        logger.entry(app, dir, resource);
        //String appIdx = searchService.appsWithSuffix(app)[0];
        logger.debug("appIdx: {}", app);

        GetResponse res = searchService.getDoc(searchService.APP_INDEX, app+"_"+dir, resource, null);
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
        
        logger.exit();
    }

}

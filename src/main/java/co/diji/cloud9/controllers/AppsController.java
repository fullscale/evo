package co.diji.cloud9.controllers;

import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.time.DateUtils;
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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
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
import co.diji.cloud9.exceptions.index.IndexException;
import co.diji.cloud9.http.RequestParams;
import co.diji.cloud9.javascript.JSGIRequest;
import co.diji.cloud9.javascript.JavascriptObject;
import co.diji.cloud9.javascript.PrimitiveWrapFactory;
import co.diji.cloud9.javascript.XMLHttpRequest;
import co.diji.cloud9.services.ConfigService;
import co.diji.cloud9.services.SearchService;

@Controller
public class AppsController {

    private static final Logger logger = LoggerFactory.getLogger(AppsController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;

    // list of static resource container/folder names
    private static final Set<String> STATIC_RESOURCES = new HashSet<String>(Arrays.asList(new String[]{
            "css", "images", "js", "html"}));

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
            String oldId = (String) data.get("from");
            String newId = (String) data.get("to");
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
    @RequestMapping(value = "/{app:[a-z0-9]+(?!(?:css|images|js|\\.))}")
    public void getResource(@PathVariable String app, HttpServletRequest request, HttpServletResponse response,
            HttpSession userSession) {
        logger.trace("in controller=app action=getResource app:{} request:{} response:{} userSession:{}", new Object[]{
                app, request, response, userSession});
        processResource(app, null, null, request, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|images|js))[a-z0-9]+}/{dir}")
    public void getResource(@PathVariable String app, @PathVariable String dir, HttpServletRequest request,
            HttpServletResponse response, HttpSession userSession) {
        logger.trace("in controller=app action=getResource app:{} dir:{} request:{} response:{} userSession:{}", new Object[]{
                app, dir, request, response, userSession});
        processResource(app, dir, null, request, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|images|js))[a-z0-9]+}/{dir}/{resource:.*}")
    public void getResource(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletRequest request, HttpServletResponse response, HttpSession userSession) {
        logger.trace("in controller=app action=getResource app:{} dir:{} resource:{} request:{} response:{} userSession:{}",
                new Object[]{app, dir, resource, request, response, userSession});
        processResource(app, dir, resource, request, response, userSession);
    }

    private void processResource(String app, String dir, String resource, HttpServletRequest request, HttpServletResponse response,
            HttpSession userSession) {
        logger.trace("in processResource app:{} dir:{} resource:{} request:{} response:{} userSession:{}", new Object[]{
                app, dir, resource, request, response, userSession});

        try {
            RequestParams params = new RequestParams(request);
            logger.debug("params: {}", params);

            // if this isn't a static resource
            boolean isStatic = STATIC_RESOURCES.contains(params.dir);
            logger.debug("isStatic: {}", isStatic);
            if (!isStatic) {

                /* controllers can potentially handle any request method */
                response.setHeader("Allow", "GET, POST, PUT, DELETE");

                params.controller = params.dir;
                params.action = params.resource;
                params.resource = params.dir + ".js";
                params.dir = "controllers";

                // fetch the controller code from the JSON store
                GetResponse doc = searchService.getDoc(params.app, params.dir, params.resource, null);
                logger.debug("doc: {}", doc);
                if (doc == null) {
                    throw new Cloud9Exception("Resource not found: " + params.resource);
                }

                // if the document (controller code) was found
                logger.debug("doc exists: {}", doc.exists());
                if (doc.exists()) {
                    Map<String, Object> source = doc.sourceAsMap();
                    logger.debug("source: {}", source);

                    String script = (String) source.get("code");
                    logger.debug("script: {}", script);

                    JSGIRequest jsgi = new JSGIRequest(request, params, userSession);

                    JavascriptObject jsResponse = null;
                    try {
                        // run the javascript controller/action code
                        jsResponse = evaluateJavascript(script, jsgi);

                        // check for an error state/code (404, 500, etc.)
                        boolean hasError = jsResponse.has("error");
                        logger.debug("hasError: {}", true);
                        if (hasError) {

                            String errorCode = jsResponse.get("error");
                            String errorMsg = jsResponse.get("errorMsg");
                            logger.debug("errorCode: {}", errorCode);
                            logger.debug("errorMsg: {}", errorMsg);

                            if (errorCode.equals("404")) {
                                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            } else {
                                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                response.getWriter().write(errorMsg);
                            }

                            // controller's action was successful
                        } else {
                            logger.debug("successfully executed controller");

                            // get content-type or set a default
                            try {
                                JavascriptObject responseHeaders = jsResponse.getObj("headers");
                                for (Map.Entry<Object, Object> header : responseHeaders.value().entrySet()) {
                                    logger.debug("set header: {}: {}", header.getKey(), header.getValue());
                                    response.setHeader((String) header.getKey(), (String) header.getValue());
                                }
                            } catch (Exception e) {
                                logger.debug("error processing controller headers", e);
                                response.setContentType("text/plain");
                            }

                            // fetch and display the response body
                            try {
                                int statusCode = 200;
                                try {
                                    statusCode = Integer.parseInt(jsResponse.get("status"));
                                } catch (Exception e) {
                                }
                                logger.debug("statusCode: {}", statusCode);
                                response.setStatus(statusCode);

                                /* JSGI Spec v0.2 returns an array */
                                NativeArray contentBody = jsResponse.getArray("body");
                                if (contentBody != null) {
                                    @SuppressWarnings("unchecked")
                                    Iterator<String> iter = contentBody.iterator();
                                    while (iter.hasNext()) {
                                        Object data = iter.next();
                                        logger.debug("writing data: {}", data);
                                        response.getWriter().write(data.toString());
                                    }
                                } else {
                                    logger.debug("null content body");
                                }
                            } catch (Exception e) {
                                logger.debug("error processing response body", e);
                            }
                        }

                        // an unknown error occurred trying to execute the Javascript code - return a 500
                    } catch (Exception e) {
                        logger.debug("Error executing javascript", e);
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }

                    // document (controller code) wasn't found - return a 404
                } else {
                    logger.debug("Unable to find controller: {}", params.resource);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }

                // the request was a "static" file
            } else {

                /* static resources only support HTTP GET */
                response.setHeader("Allow", "GET");

                boolean isGet = "GET".equals(request.getMethod());
                logger.debug("isGet: {}", isGet);
                if (!isGet) {
                    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }

                // fetch the resource from the JSON store
                GetResponse doc = searchService.getDoc(params.app, params.dir, params.resource, new String[]{
                        "_timestamp", "_source"});
                logger.debug("doc: {}", doc);
                if (doc == null) {
                    throw new Cloud9Exception("Resource not found: " + params.resource);
                }

                // if the resource was found
                logger.debug("doc exists: {}", doc.exists());
                if (doc.exists()) {

                    try {
                        /* support for conditional requests */
                        String conditionalHeader = request.getHeader("If-Modified-Since");
                        logger.debug("conditionalHeader: {}", conditionalHeader);

                        /* client/browser only supports resolutions in seconds */
                        Date lastModified = DateUtils.truncate(new Date((Long) doc.field("_timestamp").value()), Calendar.SECOND);
                        logger.debug("lastModified: {}", lastModified);

                        /*
                         * Build an expiration date 1 year from now. This is the max duration according to RFC guidelines
                         */
                        Calendar now = Calendar.getInstance();
                        now.add(Calendar.YEAR, 1);
                        Date expiration = now.getTime();
                        logger.debug("expiration: {}", expiration);

                        /* all dates should be in GMT */
                        DateFormat dateFormatter = new SimpleDateFormat("EEE, d MMM yyyy H:mm:ss z");
                        dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT:00"));

                        /* set cache headers */
                        response.addHeader("Cache-Control", "max-age=31556926, public"); // 1 year
                        response.addHeader("Expires", dateFormatter.format(expiration)); // 1 year
                        response.addHeader("Last-Modified", dateFormatter.format(lastModified));

                        /* does the client have a cached copy */
                        if (conditionalHeader != null) {
                            Date sinceModified = dateFormatter.parse(conditionalHeader);
                            logger.debug("sinceModified: {}", sinceModified);

                            /* does the client cache reflect the latest version */
                            if (lastModified.equals(sinceModified) || lastModified.before(sinceModified)) {
                                logger.debug("resource not modified");
                                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                                return;
                            }
                        }
                    } catch (Exception e) {/* swallow excpetions */
                        logger.debug("Error processing conditional request", e);
                    }
                    /* end conditional request logic */

                    Map<String, Object> source = doc.sourceAsMap();
                    logger.debug("source: {}", source);

                    String mime = (String) source.get("mime");
                    String code = (String) source.get("code");
                    logger.debug("mime: {}", mime);
                    logger.debug("code: {}", code);

                    response.setContentType(mime);

                    if (mime.startsWith("image")) {
                        logger.debug("decoding base64 image");
                        byte[] octets = Base64.decodeBase64(code);
                        ServletOutputStream servletOut = response.getOutputStream();
                        response.setContentLength(octets.length);
                        servletOut.write(octets);
                        servletOut.close();
                    } else {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write(code);
                    }

                    // couldn't find the resource - return a 404
                } else {
                    logger.debug("unable to find resource: {}", params.resource);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            }
            // the index (app) doesn't exist - just swallow the error and return a 404
        } catch (Cloud9Exception e) {
            logger.debug("Error processing resource", e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            // some unknow error occurred - return a 500
        } catch (Exception e) {
            logger.debug("Unknown error processing resource", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private JavascriptObject evaluateJavascript(String script, JSGIRequest jsgi) {
        JavascriptObject jsResponse = null;
        JavascriptObject jsRequest = jsgi.env();
        String controller = jsRequest.get("controller");
        String action = jsRequest.get("action");

        // create Rhino context
        Context cx = Context.enter();
        cx.setLanguageVersion(180);
        PrimitiveWrapFactory wrapper = new PrimitiveWrapFactory();
        wrapper.setJavaPrimitiveWrap(false);
        cx.setWrapFactory(wrapper);
        cx.setOptimizationLevel(9);

        try {
            // provides access to importPackage and importClass
            Scriptable scope = new ImporterTopLevel(cx);
            ScriptableObject.defineClass(scope, XMLHttpRequest.class);
            scope.put("ServerSideC9", scope, jsRequest.value());

            try {
                // pre load apis
                cx.evaluateReader(scope, new FileReader(config.getResourceFile("/resources/js/underscore-min.js")), "underscore", 1, null);
                cx.evaluateReader(scope, new FileReader(config.getResourceFile("/resources/js/c9/c9api.min.js")), "c9api", 1, null);

                // eval the javascript code
                cx.evaluateString(scope, script, controller, 1, null);

            } catch (EvaluatorException e) {
                // exception will contain the source and line of the error
                jsResponse = new JavascriptObject();
                jsResponse.put("error", "500");
                jsResponse.put("errorMsg", e.getMessage());
                return jsResponse;
            }

            // pull out the controller function
            Object controllerObj = scope.get(controller, scope);

            // ensure it's a valid function
            if (!(controllerObj instanceof Function)) {
                jsResponse = new JavascriptObject();
                jsResponse.put("error", "400");
                jsResponse.put("errorMsg", "NOT_FOUND");
            } else {
                // call the controller function
                Object functionArgs[] = {"test"};
                Function f = (Function) controllerObj;
                ScriptableObject result = (ScriptableObject) f.call(cx, scope, scope, functionArgs);

                // actions are properties of the controller's return value
                // they should point to functions
                Object actionMethod = ScriptableObject.getProperty(result, action);

                // ensure the action property does in fact point to a function
                if (!(actionMethod instanceof Function)) {
                    jsResponse = new JavascriptObject();
                    jsResponse.put("error", "400");
                    jsResponse.put("errorMsg", "NOT_FOUND");
                } else {
                    // call the controller action
                    Object fargs[] = {jsRequest.value()};
                    Function ff = (Function) actionMethod;
                    Object response = ff.call(cx, scope, result, fargs);

                    if (response instanceof String) {
                        jsResponse = new JavascriptObject();
                        jsResponse.put("body", (String) response);
                        jsResponse.put("contentType", "text/plain");
                    } else if (response instanceof NativeObject) {
                        jsResponse = new JavascriptObject((NativeObject) response);
                    } else {
                        jsResponse = new JavascriptObject();
                        jsResponse.put("error", "500");
                        jsResponse.put("errorMsg", "INTERNAL_SERVER_ERROR");
                    }
                }
            }
            // an unknow error occurred - return a 500
        } catch (Exception ex) {
            logger.debug("error processing javascript", ex);
            jsResponse = new JavascriptObject();
            jsResponse.put("error", "500");
            jsResponse.put("errorMsg", "INTERNAL_SERVER_ERROR");

            // important to exit the Rhino context
        } finally {
            Context.exit();
        }

        return jsResponse;
    }
}

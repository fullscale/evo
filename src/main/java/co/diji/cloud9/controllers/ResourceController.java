package co.diji.cloud9.controllers;

import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.time.DateUtils;
import org.elasticsearch.action.get.GetResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import co.diji.cloud9.exceptions.Cloud9Exception;
import co.diji.cloud9.javascript.JSGIRequest;
import co.diji.cloud9.javascript.JavascriptObject;
import co.diji.cloud9.javascript.PrimitiveWrapFactory;
import co.diji.cloud9.javascript.RequestInfo;
import co.diji.cloud9.javascript.XMLHttpRequest;
import co.diji.cloud9.services.ConfigService;
import co.diji.cloud9.services.SearchService;

@Controller
public class ResourceController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;

    private static final Set<String> STATIC_RESOURCES = new HashSet<String>(Arrays.asList(new String[]{
            "css", "images", "js", "html"}));

    private ScriptableObject sharedScope;

    @PostConstruct
    private void initialize() {
        initializeSharedScope();
    }

    private Context getContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(180);
        PrimitiveWrapFactory wrapper = new PrimitiveWrapFactory();
        wrapper.setJavaPrimitiveWrap(false);
        cx.setWrapFactory(wrapper);
        cx.setOptimizationLevel(9);

        return cx;
    }

    private void initializeSharedScope() {
        Context cx = getContext();

        // provides access to importPackage and importClass
        // seal standard imports
        ScriptableObject scope = new ImporterTopLevel(cx, true);

        try {
            // for ajax calls
            ScriptableObject.defineClass(scope, XMLHttpRequest.class);

            // used in cloud9 javascript api to detect if we are running server side or not
            scope.put("ServerSideC9", scope, true);

            // resources
            cx.evaluateReader(scope,
                    new FileReader(config.getResourceFile("/resources/js/underscore-min.js")),
                    "underscore",
                    1,
                    null);
            cx.evaluateReader(scope, new FileReader(config.getResourceFile("/resources/js/c9/c9api.min.js")), "c9api", 1, null);

            // seal everything not already sealed
            scope.sealObject();
        } catch (Exception e) {
            logger.warn("Unable to load server side javascript shared scope: {}", e.getMessage(), e);
        } finally {
            Context.exit();
        }

        this.sharedScope = scope;
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
            RequestInfo params = new RequestInfo(request);
            logger.debug("params: {}", params);

            // if this isn't a static resource
            boolean isStatic = STATIC_RESOURCES.contains(params.getDir());
            logger.debug("isStatic: {}", isStatic);
            if (!isStatic) {

                /* controllers can potentially handle any request method */
                response.setHeader("Allow", "GET, POST, PUT, DELETE");

                params.setController(params.getDir());
                params.setAction(params.getResource());
                params.setResource(params.getDir() + ".js");
                params.setDir("controllers");

                // fetch the controller code from the JSON store
                GetResponse doc = searchService.getDoc(params.getApp(), params.getDir(), params.getResource(), null);
                logger.debug("doc: {}", doc);
                if (doc == null) {
                    throw new Cloud9Exception("Resource not found: " + params.getResource());
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
                    logger.debug("Unable to find controller: {}", params.getResource());
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
                GetResponse doc = searchService.getDoc(params.getApp(), params.getDir(), params.getResource(), new String[]{
                        "_timestamp", "_source"});
                logger.debug("doc: {}", doc);
                if (doc == null) {
                    throw new Cloud9Exception("Resource not found: " + params.getResource());
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
                    logger.debug("unable to find resource: {}", params.getResource());
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
        Context cx = getContext();

        try {
            // create scope based on our shared scope
            Scriptable scope = cx.newObject(sharedScope);
            scope.setPrototype(sharedScope);
            scope.setParentScope(null);

            // add request values to scope
            scope.put("REQUEST", scope, jsRequest.value());

            try {
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

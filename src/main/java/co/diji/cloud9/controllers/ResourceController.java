package co.diji.cloud9.controllers;

import java.io.BufferedOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.tika.io.IOUtils;
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
import co.diji.cloud9.exceptions.javascript.ExecutionException;
import co.diji.cloud9.exceptions.javascript.NotFoundException;
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

    /**
     * Enter's Rhino execution context
     * 
     * @return the context
     */
    private Context getContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(180);
        PrimitiveWrapFactory wrapper = new PrimitiveWrapFactory();
        wrapper.setJavaPrimitiveWrap(false);
        cx.setWrapFactory(wrapper);
        cx.setOptimizationLevel(9);

        return cx;
    }

    /**
     * Loads javascript resources into shared scope
     */
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
        logger.trace("in controller=resource action=getResource app:{} request:{} response:{} userSession:{}", new Object[]{
                app, request, response, userSession});
        processResource(app, null, null, request, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|images|js))[a-z0-9]+}/{dir}")
    public void getResource(@PathVariable String app, @PathVariable String dir, HttpServletRequest request,
            HttpServletResponse response, HttpSession userSession) {
        logger.trace("in controller=resource action=getResource app:{} dir:{} request:{} response:{} userSession:{}", new Object[]{
                app, dir, request, response, userSession});
        processResource(app, dir, null, request, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|images|js))[a-z0-9]+}/{dir}/{resource:.*}")
    public void getResource(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletRequest request, HttpServletResponse response, HttpSession userSession) {
        logger.trace("in controller=resource action=getResource app:{} dir:{} resource:{} request:{} response:{} userSession:{}",
                new Object[]{app, dir, resource, request, response, userSession});
        processResource(app, dir, resource, request, response, userSession);
    }

    /**
     * Processes a resource file (js, css, html, controller, image)
     * 
     * @param app the app the resource belongs to
     * @param dir the parent directory of the resource
     * @param resource the resource id/name
     * @param request the http request
     * @param response the http response
     * @param session the http session
     */
    private void processResource(String app, String dir, String resource, HttpServletRequest request, HttpServletResponse response,
            HttpSession session) {
        logger.trace("in controller=resource action-processResource app:{} dir:{} resource:{} request:{} response:{} session:{}",
                new Object[]{app, dir, resource, request, response, session});

        try {
            RequestInfo params = new RequestInfo(request);
            String[] fields = null; // the fields we want returned, null = default

            logger.debug("params: {}", params);

            // if this is a static resource
            boolean isStatic = STATIC_RESOURCES.contains(params.getDir());
            logger.debug("isStatic: {}", isStatic);

            if (isStatic) {
                // we only want source and timestamp from static resources
                fields = new String[]{"_timestamp", "_source"};

                // static resources only support HTTP GET
                response.setHeader("Allow", "GET");

                boolean isGet = "GET".equals(request.getMethod());
                logger.debug("isGet: {}", isGet);
                if (!isGet) {
                    logger.debug("method not allowed for static resource: {}", request.getMethod());
                    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            } else {
                // non-static resource, javascript controller
                // controllers can potentially handle any request method
                response.setHeader("Allow", "GET, POST, PUT, DELETE");

                // setup correct request information for the controller (it is different than static resources)
                params.setController(params.getDir());
                params.setAction(params.getResource());
                params.setResource(params.getDir() + ".js");
                params.setDir("controllers");
            }

            // get the resource and parse out the source document
            GetResponse doc = getDoc(params.getApp(), params.getDir(), params.getResource(), fields);
            Map<String, Object> source = doc.sourceAsMap();
            logger.debug("source: {}", source);

            if (isStatic) {
                // convert timestamp to date object
                Date lastModified = DateUtils.truncate(new Date((Long) doc.field("_timestamp").value()), Calendar.SECOND);

                // check if the resource has been modified and set cache control headers
                boolean modified = handleLastModified(request, response, lastModified);
                if (modified) {
                    // process the resource
                    handleStaticResource(request, response, source);
                } else {
                    // tell browser to use cached copy
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            } else {
                // execute the dynamic javascript controller
                handleJavascriptController(request, response, session, params, source);
            }
        } catch (Cloud9Exception e) {
            // the index (app) doesn't exist - just swallow the error and return a 404
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e);
        }
    }

    /**
     * Processes a static resource
     * 
     * @param request the http request
     * @param response the http response
     * @param doc the resource document
     */
    private void handleStaticResource(HttpServletRequest request, HttpServletResponse response, Map<String, Object> doc) {
        logger.trace("in controller=resource action=handleStaticResource request:{} response:{} doc:{}", new Object[]{
                request, response, doc});

        String mime = (String) doc.get("mime");
        logger.debug("mime: {}", mime);
        String code = (String) doc.get("code");
        logger.debug("code: {}", code);

        try {
            byte[] octets = getResourceAsBytes(mime, code);
            OutputStream out = new BufferedOutputStream(response.getOutputStream());

            response.setContentType(mime);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(octets.length);

            IOUtils.write(octets, out);
            out.close();
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }

        logger.trace("exit handleStaticResource");
    }

    /**
     * Processes a Javascript controller
     * 
     * @param request the http request
     * @param response the http response
     * @param session the http session
     * @param params the request information object
     * @param doc the source javascript document
     */
    private void handleJavascriptController(HttpServletRequest request, HttpServletResponse response, HttpSession session,
            RequestInfo params, Map<String, Object> doc) {
        logger.trace("in controller=resource action=handleJavascriptController request:{} response:{} session:{} params:{} doc:{}",
                new Object[]{request, response, session, params, doc});

        String script = (String) doc.get("code");
        logger.debug("script: {}", script);

        try {
            JSGIRequest jsgi = new JSGIRequest(request, params, session);

            // run the javascript controller/action code
            JavascriptObject jsResponse = evaluateJavascript(script, jsgi);

            // controller's action was successful
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
            int statusCode = 200;
            try {
                statusCode = Integer.parseInt(jsResponse.get("status"));
            } catch (Exception e) {
                logger.debug("Error parsing statusCode", e);
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
        } catch (NotFoundException e) {
            // not found, return 404
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e);
        } catch (ExecutionException e) {
            // runtime/execution error, return 500
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } catch (Exception e) {
            // an unknown error, return 500
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }

        logger.trace("exit handleJavascriptController");
    }

    /**
     * Gets the resource document
     * 
     * @param app the app the resources belongs to
     * @param dir the type of resource
     * @param resource the name/id of the resource
     * @param fields what fields you want returned
     * @return the document
     * @throws Cloud9Exception if resource document is not found
     */
    private GetResponse getDoc(String app, String dir, String resource, String[] fields) throws Cloud9Exception {
        logger.trace("in controller=resource action=getDoc app:{} dir:{} resource:{}, fields:{}", new Object[]{
                app, dir, resource, fields});
        GetResponse doc = searchService.getDoc(app, dir, resource, fields);
        logger.debug("doc: {}", doc);
        if (doc == null || !doc.exists()) {
            throw new Cloud9Exception("Resource not found: " + resource);
        }

        logger.trace("exit getDoc: {}", doc);
        return doc;
    }

    /**
     * Checks if a resource was modified since last request
     * 
     * Check the If-Modified-Since header and compares it with the lastModified date. Sets cache expiration headers.
     * 
     * @param request the http request to get headers from
     * @param response the http response to set cache headers for
     * @param lastModified the modification date of the resource
     * @return true if the resource was modified, false if not modified
     */
    private boolean handleLastModified(HttpServletRequest request, HttpServletResponse response, Date lastModified) {
        logger.trace("in controller=resource action=handleLastModified request:{} response:{} lastModified:{}", new Object[]{
                request, response, lastModified});

        // all dates should be in GMT
        DateFormat dateFormatter = new SimpleDateFormat("EEE, d MMM yyyy H:mm:ss z");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT:00"));
        boolean modified = true; // defaults to the resource being considered modified

        try {
            // Build an expiration date 1 year from now. This is the max duration according to RFC guidelines
            Calendar expiration = Calendar.getInstance();
            expiration.add(Calendar.YEAR, 1);
            logger.debug("expiration: {}", expiration);

            // set cache headers
            logger.debug("setting cache control headers");
            response.addHeader("Cache-Control", "max-age=31556926, public"); // 1 year
            response.addHeader("Expires", dateFormatter.format(expiration.getTime())); // 1 year
            response.addHeader("Last-Modified", dateFormatter.format(lastModified));

            // does the client have a cached copy
            String conditionalHeader = request.getHeader("If-Modified-Since");
            logger.debug("conditionalHeader: {}", conditionalHeader);
            if (conditionalHeader != null) {
                Date sinceModified = dateFormatter.parse(conditionalHeader);
                logger.debug("sinceModified: {}", sinceModified);

                // does the client cache reflect the latest version
                if (lastModified.equals(sinceModified) || lastModified.before(sinceModified)) {
                    logger.debug("resource not modified");
                    modified = false;
                }
            }
        } catch (Exception e) {/* swallow excpetions */
            logger.debug("Error processing conditional request", e);
        }

        logger.trace("exit handleLastModified");
        return modified;
    }

    /**
     * Converts the resource to a byte array based on the mime type. Images are base64 decoded and string data is converted to utf-8
     * bytes
     * 
     * @param mime the mime type of the resource/data to convert
     * @param data the resource data
     * @return the data as a byte array
     * @throws UnsupportedEncodingException if string data can't be decoded as utf-8
     */
    private byte[] getResourceAsBytes(String mime, String data) throws UnsupportedEncodingException {
        logger.trace("in controller=resource action=getResourceBytes mime:{} data:{}", mime, data);

        byte[] octets;

        if (mime.startsWith("image")) {
            logger.debug("decoding base64 image");
            octets = Base64.decodeBase64(data);
        } else {
            logger.debug("getting string as utf-8 bytes");
            octets = data.getBytes("UTF-8");
        }

        logger.trace("exit getResourceBytes");
        return octets;
    }

    /**
     * Writes error responses to http response
     * 
     * @param response the http response
     * @param code the error code
     * @param error the error
     */
    private void sendErrorResponse(HttpServletResponse response, int code, Exception error) {
        logger.debug("processing error: {}", error.getMessage(), error);
        try {
            if (code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                response.sendError(code, ExceptionUtils.getStackTrace(error));
            } else {
                response.setStatus(code);
            }
        } catch (IOException e) {
            logger.debug("Error sending error response", e);
        }
    }

    /**
     * Executes the javascript controller
     * 
     * @param script the javascript controller script to execute
     * @param jsgi the request object
     * @return response object
     * @throws ExecutionException on error executing javascript
     * @throws NotFoundException when the expected object is not found
     */
    private JavascriptObject evaluateJavascript(String script, JSGIRequest jsgi) throws ExecutionException, NotFoundException {
        logger.trace("in controller=resource action=evaluateJavascript script:{} jsgi:{}", script, jsgi);

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
                logger.debug("Error executing javascript: {}", e.getMessage(), e);
                throw new ExecutionException(e);
            }

            // pull out the controller function
            Object controllerObj = scope.get(controller, scope);

            // ensure it's a valid function
            if (!(controllerObj instanceof Function)) {
                logger.debug("Controller not a function: {}", controllerObj);
                throw new NotFoundException("Controller Not Found");
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
                    logger.debug("Action not a function: {}", actionMethod);
                    throw new NotFoundException("Action Not Found");
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
                        logger.debug("Unknown action response: {}", response);
                        throw new ExecutionException("Unknown action response");
                    }
                }
            }
        } finally {
            Context.exit();
        }

        logger.trace("exit evaluateJavascript: {}", jsResponse);
        return jsResponse;
    }

}

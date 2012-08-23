package co.diji.cloud9.apps.resources;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hazelcast.spring.context.SpringAware;

import org.elasticsearch.action.get.GetResponse;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.diji.cloud9.exceptions.resources.InternalErrorException;
import co.diji.cloud9.exceptions.resources.NotFoundException;
import co.diji.cloud9.exceptions.resources.ResourceException;
import co.diji.cloud9.javascript.JSGIRequest;
import co.diji.cloud9.javascript.JavascriptHelper;
import co.diji.cloud9.javascript.JavascriptObject;
import co.diji.cloud9.javascript.RequestInfo;

/**
 * Represents a dynamic javascript resource
 * 
 */
@Component
@SpringAware
@Scope("prototype")
public class JavascriptResource extends Resource {

    private static final long serialVersionUID = -5627919602999703186L;
    protected static final Logger logger = LoggerFactory.getLogger(JavascriptResource.class);

    @Autowired
    protected transient JavascriptHelper jsHelper;

    // script are not serializable we will compile on each node
    protected transient Script script;

    // serializable
    protected String code;

    @Override
    public void setup(String app, String dir, String resource) throws ResourceException {
        super.setup(app, dir, resource);
        logger.trace("in setup");

        // get the resource doc from the app index
        GetResponse doc = getDoc(null); // null means return default field

        // get the resource source
        Map<String, Object> source = doc.sourceAsMap();

        // get the code and compile it
        code = (String) source.get("code");
        script = compileScript(code);

        logger.trace("exit setup");
    }

    /**
     * Compiles the code into a script object
     * 
     * @param code the code to compile
     * @return the compiled script
     */
    protected Script compileScript(String code) {
        logger.trace("in compileScript");
        Context cx = jsHelper.getContext();
        Script script = null;

        try {
            script = cx.compileString(code, resource, 1, null);
        } finally {
            Context.exit();
        }

        logger.trace("exit compileScript");
        return script;
    }

    /**
     * @return the compiled script
     */
    public Script getScript() {
        if (script == null) {
            script = compileScript(code);
        }

        return script;
    }

    /**
     * @param script the compile script to set
     */
    public void setScript(Script script) {
        this.script = script;
    }

    /*
     * (non-Javadoc)
     * @see co.diji.cloud9.apps.resources.Resource#process(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession)
     */
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws ResourceException {
        logger.trace("in process");

        // controllers can potentially handle any request method
        response.setHeader("Allow", "GET, POST, PUT, DELETE");

        JSGIRequest jsgi = new JSGIRequest(request, getRequestInfo(request), session);

        // run the javascript controller/action code
        JavascriptObject jsResponse = evaluateJavascript(jsgi);

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
            try {
                @SuppressWarnings("unchecked")
                Iterator<String> iter = contentBody.iterator();
                while (iter.hasNext()) {
                    Object data = iter.next();
                    response.getWriter().write(data.toString());
                }
            } catch (IOException e) {
                logger.debug("Error writing response", e);
                throw new InternalErrorException("Error writing response", e);
            }
        } else {
            logger.debug("null content body");
        }

        logger.trace("exit process");
    }

    public RequestInfo getRequestInfo(HttpServletRequest request) {
        logger.trace("in getRequestInfo");

        RequestInfo params = new RequestInfo(request);

        // reset some of the parsed params for our dynamic controller
        params.setController(params.getDir());
        params.setAction(params.getResource());
        params.setResource(resource);
        params.setDir(dir);

        logger.trace("exit getRequestInfo");
        return params;
    }

    /**
     * Executes the javascript controller
     * 
     * @param script the javascript controller script to execute
     * @param jsgi the request object
     * @return response object
     * @throws InternalErrorException on error executing javascript
     * @throws NotFoundException when the expected object is not found
     */
    private JavascriptObject evaluateJavascript(JSGIRequest jsgi) throws ResourceException {
        logger.trace("in evaluateJavascript");

        JavascriptObject jsResponse = null;
        JavascriptObject jsRequest = jsgi.env();
        String controller = jsRequest.get("controller");
        String action = jsRequest.get("action");

        // create Rhino context
        Context cx = jsHelper.getContext();

        try {
            // create scope based on our shared scope
            Scriptable scope = cx.newObject(jsHelper.getSharedScope());
            scope.setPrototype(jsHelper.getSharedScope());
            scope.setParentScope(null);

            // add request values to scope
            scope.put("REQUEST", scope, jsRequest.value());

            // run the compiled javascript code
            getScript().exec(cx, scope);

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
                        throw new InternalErrorException("Unknown action response");
                    }
                }
            }
        } finally {
            Context.exit();
        }

        logger.trace("exit evaluateJavascript");
        return jsResponse;
    }

    /*
     * (non-Javadoc)
     * @see co.diji.cloud9.apps.resources.Resource#readData(java.io.DataInput)
     */
    @Override
    public void readData(DataInput in) throws IOException {
        super.readData(in);
        code = in.readUTF();
    }

    /*
     * (non-Javadoc)
     * @see co.diji.cloud9.apps.resources.Resource#writeData(java.io.DataOutput)
     */
    @Override
    public void writeData(DataOutput out) throws IOException {
        super.writeData(out);
        out.writeUTF(code);
    }

}

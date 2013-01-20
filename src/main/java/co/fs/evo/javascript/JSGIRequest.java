package co.fs.evo.javascript;

import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.mozilla.javascript.NativeArray;

/*
 * This object represents a JSGI compliant interface.
 *
 * JSGI is an interface between web servers and JavaScript-based 
 * web applications and frameworks.
 *
 * This complies to the v0.3 specification.
 * http://wiki.commonjs.org/wiki/JSGI/Level0/A/Draft2
 */
public class JSGIRequest {

    private JavascriptObject req = null;

    public JSGIRequest(RequestInfo requestInfo, HttpSession userSession) {

        req = new JavascriptObject();

        /* create the JSGI environment */
        req.put("method", requestInfo.getMethod());
        req.put("controller", requestInfo.getController());
        req.put("scriptName", "/" + requestInfo.getAppname());
        req.put("pathInfo", "/" + requestInfo.getController() + "/" + requestInfo.getAction());
        req.put("action", requestInfo.getAction());
        req.put("headers", headers(requestInfo));
        req.put("queryString", requestInfo.getQueryString());
        req.put("host", requestInfo.getServer());
        req.put("port", requestInfo.getPort());
        req.put("params", params(requestInfo));
        req.put("jsgi", jsgi());
        req.put("input", body(requestInfo));
        req.put("scheme", requestInfo.getScheme());
        req.put("env", env(requestInfo));

        /* session aren't part of the spec but we can add them */
        JavascriptObject session = session(userSession);
        if (session == null) {
            req.put("session", "");
        } else {
            req.put("session", session(userSession));
        }
    }

    /*
     * creates the env specific request variables.
     */
    private JavascriptObject env(RequestInfo request) {

        JavascriptObject env = new JavascriptObject();
        return env;
    }

    /*
     * creates the jsgi specific request variables.
     */
    private JavascriptObject jsgi() {

        JavascriptObject jsgi = new JavascriptObject();
        Integer[] jsgiVersion = new Integer[]{0, 3};
        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        jsgi.put("version", new NativeArray(jsgiVersion));
        jsgi.put("cgi", false);
        jsgi.put("multithread", true);
        jsgi.put("multiprocess", false);
        jsgi.put("runOnce", false);
        jsgi.put("ext", "");
        jsgi.put("errors", writer);

        return jsgi;
    }

    /*
     * creates the header specific request variables.
     */
    private JavascriptObject headers(RequestInfo request) {
        JavascriptObject headers = new JavascriptObject();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    /*
     * creates the session specific request variables.
     */
    @SuppressWarnings("unchecked")
    private JavascriptObject session(HttpSession userSession) {
        JavascriptObject session = null;
        Map<String, String> user = (Map<String, String>) userSession.getAttribute("user");
        if (user != null) {
            session = new JavascriptObject();
            session.put("user", (String) user.get("name"));
            session.put("role", (String) user.get("role"));
            session.put("id", (String) user.get("id"));
        }
        return session;
    }

    /*
     * creates the input stream from which request the body is read.
     */
    private co.fs.evo.javascript.BufferedReader body(RequestInfo request) {
        BufferedReader reader = request.getReader();
        
        if (reader != null) {
        	return new co.fs.evo.javascript.BufferedReader(reader);
        } else {
            return new co.fs.evo.javascript.BufferedReader();
        }
    }

    /*
     * creates the parameter specific request variables.
     */
    private JavascriptObject params(RequestInfo params) {
        JavascriptObject jsParams = new JavascriptObject();

        /* wraps the URL parameters in Javascript Native objects */
        for (Map.Entry<String, String[]> entry : params.getParams().entrySet()) {
            String[] paramVals = (String[]) entry.getValue();

            if (paramVals.length == 1) {
                jsParams.put(entry.getKey(), paramVals[0]);
            } else {
                jsParams.put(entry.getKey(), new NativeArray(paramVals));
            }
        }
        return jsParams;
    }

    /*
     * returns the JSGI environment object.
     */
    public JavascriptObject env() {
        return req;
    }

}
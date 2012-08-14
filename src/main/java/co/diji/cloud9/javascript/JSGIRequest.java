package co.diji.cloud9.javascript;

import co.diji.cloud9.javascript.JavascriptObject;
import co.diji.cloud9.javascript.ByteArrayOutputStream;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.Map;

import org.mozilla.javascript.*;

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

	public JSGIRequest(HttpServletRequest request, 
                       RequestParams params, 
                       HttpSession userSession) {

        req = new JavascriptObject();

        /* create the JSGI environment */
        req.put("method", request.getMethod());
        req.put("controller", params.controller);
        req.put("scriptName", "/" + params.appname);
        req.put("pathInfo", "/" + params.controller + "/" + params.action);
        req.put("action", params.action);
        req.put("headers", headers(request));
        req.put("queryString", request.getQueryString());
        req.put("host", params.server);
        req.put("port", params.port);
        req.put("params", params(params));
        req.put("jsgi", jsgi());
        req.put("input", body(request));
        req.put("scheme", params.scheme);
        req.put("env", env(request));

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
    private JavascriptObject env(HttpServletRequest request) {

        JavascriptObject env = new JavascriptObject();
        return env;
    }

    /*
     * creates the jsgi specific request variables.
     */
    private JavascriptObject jsgi() {

        JavascriptObject jsgi = new JavascriptObject();
        Integer[] jsgiVersion = new Integer[] {0, 3};
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
	private JavascriptObject headers(HttpServletRequest request) {
        JavascriptObject headers = new JavascriptObject();
        Enumeration<String> headerNames = request.getHeaderNames();

        while(headerNames.hasMoreElements()) {
        String headerName = (String)headerNames.nextElement();
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
    private co.diji.cloud9.javascript.BufferedReader body(HttpServletRequest request) {
        try {
            BufferedReader reader = request.getReader(); 
            return new co.diji.cloud9.javascript.BufferedReader(reader);
        } catch (java.io.IOException e) {
            return new co.diji.cloud9.javascript.BufferedReader();
        }
    }

    /*
     * creates the parameter specific request variables.
     */
    private JavascriptObject params(RequestParams params) {
        JavascriptObject jsParams = new JavascriptObject();

        /* wraps the URL parameters in Javascript Native objects */
        for (Map.Entry<String, String[]> entry : params.params.entrySet()) {
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
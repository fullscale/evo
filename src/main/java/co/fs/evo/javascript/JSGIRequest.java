package co.fs.evo.javascript;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import org.mozilla.javascript.NativeArray;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import co.fs.evo.security.EvoUser;

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

	private static final XLogger logger = XLoggerFactory.getXLogger(JSGIRequest.class);
    private JavascriptObject req = null;

    public JSGIRequest(RequestInfo requestInfo, EvoUser userDetails) {

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
        //String xxx = userSession.getAttribute("SPRING_SECURITY_CONTEXT");
        if (userDetails == null) {
            req.put("session", "");
        } else {
            req.put("session", session(userDetails));
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
    private JavascriptObject session(EvoUser userDetails) {
    	
    	logger.entry("Found autheticated user");
        JavascriptObject session = null;
        
        session = new JavascriptObject();
        session.put("user", userDetails.getUsername());
        session.put("id", userDetails.getUid());
        session.put("enabled", userDetails.isEnabled());
        session.put("expired", !userDetails.isAccountNonExpired());
        session.put("locked", !userDetails.isAccountNonLocked());
            
        ArrayList<SimpleGrantedAuthority> roles = (ArrayList<SimpleGrantedAuthority>)userDetails.getAuthorities();
        String [] authorities = new String[roles.size()];
        int idx = 0;
        for (SimpleGrantedAuthority role : roles) {
        	authorities[idx] = role.getAuthority();
        	idx += 1;
        }
        
        session.put("roles", new NativeArray(authorities));
        
        logger.exit();
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
package co.fs.evo.javascript;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class RequestInfo {
	
    private static final XLogger logger = XLoggerFactory.getXLogger(RequestInfo.class);

    private final String scheme;
    private final String server;
    private final String port;
    private final String method;
    private final String queryString;
    private final long modifiedSince;
    private String controller;
    private String action;
    private String appname;
    private String app;
    private String dir;
    private String resource;
    private BufferedReader reader;
    private Map<String, String> headers;
    private Map<String, String[]> params;

    public RequestInfo(HttpServletRequest request) {
    	logger.entry();
    	
        this.scheme = request.getScheme();
        this.server = request.getServerName();
        this.port = Integer.toString(request.getServerPort());
        this.method = request.getMethod();
        this.queryString = request.getQueryString();
        this.params = request.getParameterMap();
        this.headers = new HashMap<String, String>();
        this.modifiedSince = request.getDateHeader("If-Modified-Since");
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        
        try {
        	this.reader = request.getReader();
        } catch (java.io.IOException e) {
        	this.reader = null;
        }
        
        ParseServletPath(request);
        
        logger.exit();
    }

    private void ParseServletPath(HttpServletRequest request) {

        String path = StringUtils.strip(request.getServletPath(), "/");
        String[] components = path.split("/");
        int numPathParts = components.length;

        appname = components[0];
        app = appname + ".app";

        if (numPathParts == 1) {
            // only received an application name
            dir = "html";
            resource = "index.html";
        } else if (numPathParts == 2) {
            // received an application and resource name
            dir = "html";
            resource = components[1];
        } else if (numPathParts == 3) {
            // received application, directory, and resource
            dir = components[1];
            resource = components[2];
        }
    }

    /**
     * @return the scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @return the controller
     */
    public String getController() {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    public void setController(String controller) {
        this.controller = controller;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the appname
     */
    public String getAppname() {
        return appname;
    }

    /**
     * @param appname the appname to set
     */
    public void setAppname(String appname) {
        this.appname = appname;
    }

    /**
     * @return the app
     */
    public String getApp() {
        return app;
    }

    /**
     * @param app the app to set
     */
    public void setApp(String app) {
        this.app = app;
    }

    /**
     * @return the dir
     */
    public String getDir() {
        return dir;
    }

    /**
     * @param dir the dir to set
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * @return the params
     */
    public Map<String, String[]> getParams() {
        return params;
    }
    
    /**
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * @return the queryString
     */
    public String getQueryString() {
        return queryString;
    }
    
    /**
     * @return the input reader 
     */
    public BufferedReader getReader() {
    	return reader;
    }

    /**
     * @return the given header
     */
    public String getHeader(String name) {
    	if (headers.containsKey(name)) {
    		return headers.get(name);
    	} else {
    		return null;
    	}
    }
    
    /**
     * @return all header names
     */
    public Enumeration<String> getHeaderNames() {
    	return Collections.enumeration(headers.keySet());
    }
    
    /**
     * @return all if-modified-since as a long timestamp
     */
    public long getModifiedSince() {
    	return modifiedSince;
    }
}

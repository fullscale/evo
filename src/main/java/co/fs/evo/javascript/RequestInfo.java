package co.fs.evo.javascript;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import static co.fs.evo.Constants.*;

public class RequestInfo {
	
    private static final XLogger logger = XLoggerFactory.getXLogger(RequestInfo.class);

    private final String scheme;
    private final String server;
    private final String port;
    private final String method;
    private final String queryString;
    private final String controller;
    private final String action;
    private final String appname;
    private final String app;
    private final String dir;
    private final String resource;
    private final boolean isStatic;
    private final long modifiedSince;
    private final BufferedReader reader;
    private final Map<String, String> headers;
    private final Map<String, String[]> params;

    private RequestInfo(HttpServletRequest request, 
    					String appname, 
    					String dir, 
    					String resource) 
    {
    	logger.entry();
    	
        this.scheme = request.getScheme();
        this.server = request.getServerName();
        this.port = Integer.toString(request.getServerPort());
        this.method = request.getMethod();
        this.queryString = request.getQueryString();
        this.params = request.getParameterMap();
        this.headers = new HashMap<String, String>();
        this.modifiedSince = request.getDateHeader("If-Modified-Since");
        this.appname = appname;
        this.app = appname + ".app";
        this.isStatic = STATIC_RESOURCES.contains(dir);
        
        if (!this.isStatic()) {
            // javascript controllers are in the "server-side" type/dir and have resource name of dir + .js
            logger.debug("found javascript controller, using server-side/{}.js", dir);
            this.dir = "server-side";
            this.resource = dir + ".js";
            this.controller = dir;
            this.action = resource;
        } else {
            this.dir = dir;
            this.resource = resource;
            this.controller = null;
            this.action = null;
        }
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        
        BufferedReader r = null;
        try {
        	r = request.getReader();
        } catch (Exception e) {
        	r = new BufferedReader(new StringReader(""));
        } finally {
        	this.reader = r;
        }
        
        logger.exit();
    }
    
    public static RequestInfo valueOf(HttpServletRequest request, 
    								  String appname, 
    								  String dir, 
    								  String resource) 
    {
    	return new RequestInfo(request, appname, dir, resource);
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
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @return the appname
     */
    public String getAppname() {
        return appname;
    }

    /**
     * @return the app
     */
    public String getApp() {
        return app;
    }

    /**
     * @return the dir
     */
    public String getDir() {
        return dir;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @return a defensive copy of the params map
     */
    public Map<String, String[]> getParams() {
    	return Collections.unmodifiableMap(params);
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

    /**
     * @return returns true if resource is static
     */
    public boolean isStatic() {
    	return isStatic;
    }
    
    @Override
    public String toString() {
    	return String.format("RequestInfo Obj: (%s) %s/%s/%s", 
    			method, appname, dir, resource);
    }
}

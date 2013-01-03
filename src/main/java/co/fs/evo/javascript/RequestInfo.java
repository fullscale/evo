package co.fs.evo.javascript;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

public class RequestInfo {

    private String scheme;
    private String server;
    private String port;
    private String controller;
    private String action;
    private String appname;
    private String app;
    private String dir;
    private String resource;
    private Map<String, String[]> params;

    public RequestInfo(HttpServletRequest request) {
        ParseServletPath(request);
    }

    private void ParseServletPath(HttpServletRequest request) {

        scheme = request.getScheme();
        server = request.getServerName();
        port = Integer.toString(request.getServerPort());

        String path = StringUtils.strip(request.getServletPath(), "/");
        String[] components = path.split("/");
        int numPathParts = components.length;

        appname = components[0];
        app = appname + ".app";

        if (numPathParts == 1) {
            // only recieved an application name
            dir = "html";
            resource = "index.html";
        } else if (numPathParts == 2) {
            // recieved an application and resource name
            dir = "html";
            resource = components[1];
        } else if (numPathParts == 3) {
            // recieved application, directory, and resource
            dir = components[1];
            resource = components[2];
        }

        params = request.getParameterMap();
    }

    /**
     * @return the scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * @param scheme the scheme to set
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
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
     * @param params the params to set
     */
    public void setParams(Map<String, String[]> params) {
        this.params = params;
    }
}

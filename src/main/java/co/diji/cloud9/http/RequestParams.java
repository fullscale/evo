package co.diji.cloud9.http;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;

public class RequestParams {

    public String scheme;
    public String server;
    public String port;
	public String controller;
	public String action;
	public String appname;
	public String app;
	public String dir;
	public String resource;
	public Map<String, String[]> params;

	public RequestParams(HttpServletRequest request) {
		ParseServletPath(request);	
	}
	
	private void ParseServletPath(HttpServletRequest request) {

        scheme = request.getScheme();
        server = request.getServerName();
        port = Integer.toString(request.getServerPort());
        String path = (String)request.getAttribute("javax.servlet.forward.servlet_path");
        path = StringUtils.strip(path, "/");

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
}

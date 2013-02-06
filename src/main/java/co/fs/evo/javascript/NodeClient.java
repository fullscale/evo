package co.fs.evo.javascript;

import java.io.IOException;

import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.json.JsonParser;
import org.mozilla.javascript.json.JsonParser.ParseException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.fs.evo.http.rest.InternalRestRequest;
import co.fs.evo.services.SearchService;

@Component
public class NodeClient extends ScriptableObject {
	
    public static final long serialVersionUID = 1L;
    private static final XLogger logger = XLoggerFactory.getXLogger(NodeClient.class);
    
    static protected SearchService searchService;
    
    @Autowired
    public void setRestController(SearchService searchService) {
    	//this.searchService = searchService;
    	restController = ((InternalNode) searchService.getNode()).injector().getInstance(RestController.class);
    }
    
    
    public void jsConstructor() {}
    public String getClassName() { return "NodeClient"; }
    static protected RestController restController;
    
    
    public Object jsFunction_post(String path, Object data, NativeFunction callbackFn)   {
    	logger.info("Calling jsFunction_post");
    	String payload = data == null ? "" : data.toString();
    	
    	// the actual dive into ES
    	executeRequest("POST", path, payload, callbackFn);
    	
    	// create a simple dummy object to return...
    	Object json = null;
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();
            json = new JsonParser(cx, scope).parseValue("{\"status\":\"ok\"}");
        } catch (ParseException pe) {
        	logger.warn(pe.getMessage());
        } finally {
            Context.exit();
        }
        return json;
    }
    
    public void jsFunction_put(String path, Object data, NativeFunction callbackFn)    {/* TODO */}
    public void jsFunction_get(String path, Object data, NativeFunction callbackFn)    {/* TODO */}
    public void jsFunction_delete(String path, Object data, NativeFunction callbackFn) {/* TODO */}
    
    private void executeRequest(String method, String uri, String payload, NativeFunction callbackFn) {
    	// build a RestRequest and RestChannel objects
    	logger.info("executing request...");
    	InternalRestRequest restRequest = new InternalRestRequest(method, uri, payload);
    	InternalRestChannel channel = new InternalRestChannel(restRequest, callbackFn, this);
    	if (restController != null) {
    		restController.dispatchRequest(restRequest, channel); 
    		logger.info("request executed...");
    	} else {
    		logger.info("Unable to execute request... restController was null");
    	}
    }
    
    static class InternalRestChannel implements RestChannel {
    	private final RestRequest restRequest;
    	private final NativeFunction callbackFn;
    	private final NodeClient nc;
    	
        InternalRestChannel(RestRequest restRequest, NativeFunction callbackFn, NodeClient scope) {
        	logger.info("Inside InternalRestChannel...");
            this.restRequest = restRequest;
            this.callbackFn = callbackFn;
            this.nc = scope;
            logger.info("Exiting InternalRestChannel constructor...");
        }
        
        @Override
        public void sendResponse(RestResponse response) {
        	logger.info("sendResponse called");

            Object json = null;
            try {
            	String jsonString = new String(response.content(), 0, response.contentLength(), "UTF-8");
            	logger.info("Response Content: " + jsonString);
            	
	            if (response != null && response.contentLength() > 0) {
	                Context cx = Context.enter();
	                try {
	                    Scriptable scope = cx.initStandardObjects();
	                    json = new JsonParser(cx, scope).parseValue(jsonString);
	                    if (this.callbackFn != null) {
	                    	this.callbackFn.call(cx, scope, this.nc, new Object[] {json});
	                    }
	                } catch (ParseException pe) {
	                	logger.warn(pe.getMessage());
	                } finally {
	                    Context.exit();
	                }
	            }
            } catch (IOException ioe) {
            	logger.info(ioe.getMessage());
            }
            logger.info("exiting sendResponse...");
    	}
    }

}

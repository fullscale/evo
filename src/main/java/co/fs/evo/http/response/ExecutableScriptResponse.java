package co.fs.evo.http.response;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.get.GetResponse;
import org.mozilla.javascript.NativeArray;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.hazelcast.spring.context.SpringAware;

import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.http.request.RequestInfo;
import co.fs.evo.javascript.JSGIRequest;
import co.fs.evo.javascript.JavascriptObject;
import co.fs.evo.security.EvoUser;
import co.fs.evo.services.ScriptEngine;
import co.fs.evo.services.SearchService;

@Component
@SpringAware
@Scope("prototype")
public class ExecutableScriptResponse implements Response {

	private static final long serialVersionUID = 8812494999737600581L;
    private static final XLogger logger = XLoggerFactory.getXLogger(ExecutableScriptResponse.class);

    @Autowired protected transient ScriptEngine scriptEngine;
    @Autowired protected transient SearchService searchService;

    // serializable
    protected String code;

	@Override
	public void send(RequestInfo request, AsyncContext ctx, EvoUser userDetails)
			throws ResourceException {

        logger.entry();
        
        HttpServletResponse response = (HttpServletResponse)ctx.getResponse();

        // controllers can potentially handle any request method
        logger.debug("send allow headers");
        response.setHeader("Allow", "GET, POST, PUT, DELETE");

        logger.debug("creating jsgi request");
        JSGIRequest jsgi = new JSGIRequest(request, userDetails);

        // run the javascript controller/action code
        logger.debug("evaluating javascript");
        String key = request.getAppname() + request.getDir() + request.getResource();
        JavascriptObject jsResponse = scriptEngine.evaluateJavascript(jsgi, code, key);

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
            } finally {
            	try {
					response.flushBuffer();
				} catch (IOException e) {
					logger.debug("Unable to flush buffer");
				}
                ctx.complete();
            }
        } else {
            logger.debug("null content body");
            try {
				response.flushBuffer();
			} catch (IOException e) {
				logger.debug("Unable to flush buffer");
			}
            ctx.complete();
        }

        logger.exit();
	}

	@Override
	public void loadResource(RequestInfo requestInfo) 
			throws NotFoundException, InternalErrorException {
		
        logger.entry();

        GetResponse response = searchService.
        		getAppResource(requestInfo.getAppname(), 
				   			   requestInfo.getDir(), 
				   			   requestInfo.getResource(), 
				   			   new String[]{"_timestamp", "_source"});
        
        if (response == null || !response.exists()) {
            throw new NotFoundException("Resource not found: " + requestInfo);
        }
        
        code = (String) response.sourceAsMap().get("code");
        
        logger.trace("Raw resource: {}", code);
        logger.exit();	
	}

	@Override
	public void readData(DataInput in) throws IOException {
        code = in.readUTF();
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
        out.writeUTF(code);
	}

}

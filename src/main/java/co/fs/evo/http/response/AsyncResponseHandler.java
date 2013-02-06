package co.fs.evo.http.response;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.hazelcast.spring.context.SpringAware;

import co.fs.evo.apps.resources.ResourceCache;
import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotAllowedException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.http.request.RequestInfo;
import co.fs.evo.security.EvoUser;

@Component
@SpringAware
@Scope("prototype")
public class AsyncResponseHandler implements Runnable {

	private static final XLogger logger = XLoggerFactory.getXLogger(AsyncResponseHandler.class);
	
    @Autowired protected ResourceCache cache;
    @Autowired protected ApplicationContext appContext;
	
    private final RequestInfo requestInfo;
    private final AsyncContext ctx;
    private final EvoUser userDetails;
    
    public AsyncResponseHandler(RequestInfo requestInfo, 
    							AsyncContext ctx, 
    							EvoUser userDetails) {
    	
    	this.requestInfo = requestInfo;
    	this.ctx = ctx;
    	this.userDetails = userDetails;
    }
    
	@Override
	public void run() {
		logger.entry();
		
		try {
			logger.trace("Processing async request: {}", requestInfo);

            String cacheKey = cache.getCacheKey(requestInfo.getAppname(), 
            		requestInfo.getDir(), requestInfo.getResource());
            
            logger.debug("Checking for cached response: {}", cacheKey);
            Response response = cache.getResponse(cacheKey);

            if (response == null) {
                logger.debug("Cache Miss: {}", requestInfo);
                response = appContext.getBean(requestInfo.isStatic() ? 
                		StaticResourceResponse.class : 
                		ExecutableScriptResponse.class);
                
                response.loadResource(requestInfo);
                cache.putResponse(cacheKey, response);
            }
            
            response.send(requestInfo, ctx, userDetails);
            
        } catch (NotFoundException e) {
            sendErrorResponse(ctx, HttpServletResponse.SC_NOT_FOUND, e);
            
        } catch (InternalErrorException e) {
            logger.warn(e.getMessage());
            sendErrorResponse(ctx, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            
        } catch (NotAllowedException e) {
            sendErrorResponse(ctx, HttpServletResponse.SC_METHOD_NOT_ALLOWED, e);
            
        } catch (ResourceException e) {
            logger.warn(e.getMessage());
            sendErrorResponse(ctx, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
		logger.exit();
	}
	
    private void sendErrorResponse(AsyncContext ctx, int statusCode, Exception error) {
        logger.entry(statusCode, error);
        HttpServletResponse response = (HttpServletResponse)ctx.getResponse();
        try {
            logger.debug("Sending error response: {}", error);
            if (statusCode == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                response.sendError(statusCode, ExceptionUtils.getStackTrace(error));
            } else {
                response.setStatus(statusCode);
            }
        } catch (IOException e) {
            logger.debug("Unable to send error response: {}", e);
        } finally {
        	ctx.complete();
        }
        logger.exit();
    }
}

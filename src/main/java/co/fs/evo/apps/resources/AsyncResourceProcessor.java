package co.fs.evo.apps.resources;

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

import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotAllowedException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.javascript.RequestInfo;
import co.fs.evo.security.EvoUser;

@Component
@SpringAware
@Scope("prototype")
public class AsyncResourceProcessor implements Runnable {

	private static final XLogger logger = XLoggerFactory.getXLogger(AsyncResourceProcessor.class);
	
    @Autowired
    protected ResourceCache cache;
    
    @Autowired
    protected ApplicationContext appContext;
	
    private final RequestInfo requestInfo;
    private final AsyncContext ctx;
    private final EvoUser userDetails;
    
    public AsyncResourceProcessor(
    	RequestInfo requestInfo, 
    	AsyncContext ctx, 
    	EvoUser userDetails
    ) {
    	this.requestInfo = requestInfo;
    	this.ctx = ctx;
    	this.userDetails = userDetails;
    }
    
	@Override
	public void run() {
		try {
            logger.debug("getting resource");
            Resource r = getResource();
            logger.debug("processing resource request: {}", requestInfo);
            r.process(requestInfo, ctx, userDetails);
            logger.debug("done processing resource request");
        } catch (NotFoundException e) {
            sendErrorResponse(ctx, HttpServletResponse.SC_NOT_FOUND, e);
        } catch (InternalErrorException e) {
            logger.debug(e.getMessage());
            sendErrorResponse(ctx, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } catch (NotAllowedException e) {
            sendErrorResponse(ctx, HttpServletResponse.SC_METHOD_NOT_ALLOWED, e);
        } catch (ResourceException e) {
            logger.debug(e.getMessage());
            sendErrorResponse(ctx, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }		
	}
	
    private void sendErrorResponse(AsyncContext ctx, int code, Exception error) {
        logger.entry(code, error);
        HttpServletResponse response = (HttpServletResponse)ctx.getResponse();
        try {
            logger.debug("exception", error);
            if (code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                response.sendError(code, ExceptionUtils.getStackTrace(error));
            } else {
                response.setStatus(code);
            }
        } catch (IOException e) {
            logger.debug("Error sending error response", e);
        } finally {
        	ctx.complete();
        }
    }

    /**
     * Responsible for retrieving the specific resource. Attempts to get 
     * from cache first, if not found, it creates new instance.
     * 
     * @param  requestInfo The object containing all the request related details
     * @return the resource
     * @throws ResourceException on error retrieving/creating the resource
     */
    public Resource getResource() throws ResourceException {
    	String app = requestInfo.getAppname();
    	String dir = requestInfo.getDir();
    	String resource = requestInfo.getResource();
    	
        logger.entry(app, dir, resource);

        Resource r;

        // check cache
        String cacheKey = cache.getCacheKey(app, dir, resource);
        logger.debug("cacheKey: {}", cacheKey);
        r = cache.getResource(cacheKey);

        if (r == null) {
            // cache miss
            logger.debug("resource not cached");

            r = appContext.getBean(requestInfo.isStatic() ? StaticResource.class : JavascriptResource.class);
            r.loadFromDisk(app, dir, resource);
            cache.putResource(cacheKey, r);
        }

        logger.exit();
        return r;
    }
}

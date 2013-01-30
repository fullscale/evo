package co.fs.evo.apps.resources;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotAllowedException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.javascript.RequestInfo;
import co.fs.evo.security.EvoUser;

public class AsyncResourceProcessor implements Runnable {

	private static final XLogger logger = XLoggerFactory.getXLogger(AsyncResourceProcessor.class);
	
    private final RequestInfo requestInfo;
    private final AsyncContext ctx;
    private final EvoUser userDetails;
    private final ResourceHelper resourceHelper;
    
    public AsyncResourceProcessor(
    	RequestInfo requestInfo, 
    	AsyncContext ctx, 
    	EvoUser userDetails, 
    	ResourceHelper resourceHelper
    ) {
    	this.requestInfo = requestInfo;
    	this.ctx = ctx;
    	this.userDetails = userDetails;
    	this.resourceHelper = resourceHelper;
    }
    
	@Override
	public void run() {
		try {
            logger.debug("getting resource");
            Resource r = resourceHelper.getResource(requestInfo);
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

}

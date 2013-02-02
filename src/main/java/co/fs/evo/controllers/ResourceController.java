package co.fs.evo.controllers;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import co.fs.evo.apps.resources.AsyncResourceProcessor;
import co.fs.evo.javascript.RequestInfo;
import co.fs.evo.security.EvoUser;
import co.fs.evo.services.ConfigService;
import co.fs.evo.services.SearchService;

@Controller
public class ResourceController {

    private static final XLogger logger = XLoggerFactory.getXLogger(ResourceController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;
    
    @Autowired 
    protected TaskExecutor taskExecutor;
    
    @Autowired
    protected ApplicationContext appContext;
    
    @ResponseBody
    @RequestMapping(value = "/{app:[a-z0-9]+(?!(?:css|img|js|\\.))}")
    public void getResource(@PathVariable String app, HttpServletRequest request, HttpServletResponse response,
            HttpSession userSession) {
    	
        logger.entry();
        processAsyncRequest(request, response, app, "html", "index.html");
        logger.exit();
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|img|js))[a-z0-9]+}/{dir}")
    public void getResource(@PathVariable String app, @PathVariable String dir, HttpServletRequest request,
            HttpServletResponse response, HttpSession userSession) {
    	
        logger.entry();
        processAsyncRequest(request, response, app, "html", dir + ".html");
        logger.exit();
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|img|js))[a-z0-9]+}/{dir}/{resource:.*}")
    public void getResource(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletRequest request, HttpServletResponse response, HttpSession userSession) {
        
    	logger.entry();
        processAsyncRequest(request, response, app, dir, resource);
        logger.exit();
    }
    
    /**
     * Creates an asynchronous request handler for processing resources
     * 
     * @param request the http request
     * @param response the http response
     * @param app the target application
     * @param dir the target content-type
     * @param resource the target resource
     */
    private void processAsyncRequest(HttpServletRequest request, 
    							     HttpServletResponse response, 
    							     String app, 
    							     String dir, 
    							     String resource) {
    	
    	// gather security details
    	EvoUser userDetails = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof AnonymousAuthenticationToken)) {
        	userDetails = (EvoUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
        
        // execute an AsyncResourceProcessor task in a new thread
        final AsyncContext asyncContext = request.startAsync(request, response);
        try {
            RequestInfo requestInfo = RequestInfo.valueOf(request, app, dir, resource);
            if (this.taskExecutor != null) {
            	AsyncResourceProcessor resourceProcessor = new AsyncResourceProcessor(
            			requestInfo, asyncContext, userDetails);
            	
            	// explicitly autowire this instance
            	appContext.getAutowireCapableBeanFactory().autowireBean(resourceProcessor);
            	
            	this.taskExecutor.execute(resourceProcessor);
            }
        } catch (Exception e) {
            logger.error("Error processing resource", e);
            response.setStatus(500);
            asyncContext.complete();
        }    	
    }
}

package co.fs.evo.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import co.fs.evo.apps.resources.Resource;
import co.fs.evo.apps.resources.ResourceHelper;
import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotAllowedException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.javascript.RequestInfo;
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
    protected ResourceHelper resourceHelper;
    
    @ResponseBody
    @RequestMapping(value = "/{app:[a-z0-9]+(?!(?:css|img|js|\\.))}")
    public void getResource(@PathVariable String app, HttpServletRequest request, HttpServletResponse response,
            HttpSession userSession) {
        logger.entry();
        
        // convert the HttpServletRequest to a thread safe RequestInfo object
        RequestInfo requestInfo = getRequestInfo(request, null, null);
        processResource(app, null, null, requestInfo, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|img|js))[a-z0-9]+}/{dir}")
    public void getResource(@PathVariable String app, @PathVariable String dir, HttpServletRequest request,
            HttpServletResponse response, HttpSession userSession) {
        logger.entry();
        
        // convert the HttpServletRequest to a thread safe RequestInfo object
        RequestInfo requestInfo = getRequestInfo(request, dir, null);
        processResource(app, dir, null, requestInfo, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|img|js))[a-z0-9]+}/{dir}/{resource:.*}")
    public void getResource(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletRequest request, HttpServletResponse response, HttpSession userSession) {
        logger.entry();
        
        // convert the HttpServletRequest to a thread safe RequestInfo object
        RequestInfo requestInfo = getRequestInfo(request, dir, resource);
        processResource(app, dir, resource, requestInfo, response, userSession);
    }

    /**
     * Processes a resource file (js, css, html, controller, image)
     * 
     * @param app the app the resource belongs to
     * @param dir the parent directory of the resource
     * @param resource the resource id/name
     * @param request the http request
     * @param response the http response
     * @param session the http session
     */
    private void processResource(String app, String dir, String resource, RequestInfo request, HttpServletResponse response,
            HttpSession session) {
        logger.entry(app, dir, resource);

        try {
            logger.debug("getting resource");
            Resource r = resourceHelper.getResource(app, dir, resource);
            logger.debug("processing resource request");
            r.process(request, response, session);
            logger.debug("done processing resource request");
        } catch (NotFoundException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e);
        } catch (InternalErrorException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } catch (NotAllowedException e) {
            sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, e);
        } catch (ResourceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }

        logger.exit();
    }

    /**
     * Writes error responses to http response
     * 
     * @param response the http response
     * @param code the error code
     * @param error the error
     */
    private void sendErrorResponse(HttpServletResponse response, int code, Exception error) {
        logger.entry(code, error);
        try {
            logger.debug("exception", error);
            if (code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                response.sendError(code, ExceptionUtils.getStackTrace(error));
            } else {
                response.setStatus(code);
            }
        } catch (IOException e) {
            logger.debug("Error sending error response", e);
        }
    }
    
    /**
     * Gets the RequestInfo object with parameters set correctly
     * for javascript controllers.
     * 
     * @param request the http request object
     * @return the request info object
     */
    public RequestInfo getRequestInfo(HttpServletRequest request, String dir, String resource) {
        logger.entry();

        RequestInfo requestInfo = new RequestInfo(request);

        // reset some of the parsed params for our dynamic controller
        requestInfo.setController(requestInfo.getDir());
        requestInfo.setAction(requestInfo.getResource());
        requestInfo.setResource(resource);
        requestInfo.setDir(dir);

        logger.exit();
        return requestInfo;
    }

}

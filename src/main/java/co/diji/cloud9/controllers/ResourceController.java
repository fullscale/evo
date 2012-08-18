package co.diji.cloud9.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import co.diji.cloud9.apps.resources.Resource;
import co.diji.cloud9.apps.resources.ResourceHelper;
import co.diji.cloud9.exceptions.resources.InternalErrorException;
import co.diji.cloud9.exceptions.resources.NotAllowedException;
import co.diji.cloud9.exceptions.resources.NotFoundException;
import co.diji.cloud9.exceptions.resources.ResourceException;
import co.diji.cloud9.services.ConfigService;
import co.diji.cloud9.services.SearchService;

@Controller
public class ResourceController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;

    @Autowired
    protected ResourceHelper resourceHelper;
    
    @ResponseBody
    @RequestMapping(value = "/{app:[a-z0-9]+(?!(?:css|images|js|\\.))}")
    public void getResource(@PathVariable String app, HttpServletRequest request, HttpServletResponse response,
            HttpSession userSession) {
        logger.trace("in controller=resource action=getResource app:{} request:{} response:{} userSession:{}", new Object[]{
                app, request, response, userSession});
        processResource(app, null, null, request, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|images|js))[a-z0-9]+}/{dir}")
    public void getResource(@PathVariable String app, @PathVariable String dir, HttpServletRequest request,
            HttpServletResponse response, HttpSession userSession) {
        logger.trace("in controller=resource action=getResource app:{} dir:{} request:{} response:{} userSession:{}", new Object[]{
                app, dir, request, response, userSession});
        processResource(app, dir, null, request, response, userSession);
    }

    @ResponseBody
    @RequestMapping(value = "/{app:(?!(?:css|images|js))[a-z0-9]+}/{dir}/{resource:.*}")
    public void getResource(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletRequest request, HttpServletResponse response, HttpSession userSession) {
        logger.trace("in controller=resource action=getResource app:{} dir:{} resource:{} request:{} response:{} userSession:{}",
                new Object[]{app, dir, resource, request, response, userSession});
        processResource(app, dir, resource, request, response, userSession);
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
    private void processResource(String app, String dir, String resource, HttpServletRequest request, HttpServletResponse response,
            HttpSession session) {
        logger.trace("in controller=resource action-processResource app:{} dir:{} resource:{} request:{} response:{} session:{}",
                new Object[]{app, dir, resource, request, response, session});

        try {
            Resource r = resourceHelper.getResource(app, dir, resource);
            r.process(request, response, session);
        } catch (NotFoundException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e);
        } catch (InternalErrorException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } catch (NotAllowedException e) {
            sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, e);
        } catch (ResourceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }

    }

    /**
     * Writes error responses to http response
     * 
     * @param response the http response
     * @param code the error code
     * @param error the error
     */
    private void sendErrorResponse(HttpServletResponse response, int code, Exception error) {
        logger.debug("processing error: {}", error.getMessage(), error);
        try {
            if (code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                response.sendError(code, ExceptionUtils.getStackTrace(error));
            } else {
                response.setStatus(code);
            }
        } catch (IOException e) {
            logger.debug("Error sending error response", e);
        }
    }

}

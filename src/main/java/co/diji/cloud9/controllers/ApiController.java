package co.diji.cloud9.controllers;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.support.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import co.diji.cloud9.rest.ServletRestRequest;
import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/v1")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    protected SearchService searchService;

    protected RestController restController;

    @PostConstruct
    public void init() {
        restController = ((InternalNode) searchService.getNode()).injector().getInstance(RestController.class);
    }

    @ResponseBody
    @RequestMapping(value = "/apps/{app}", method = RequestMethod.GET)
    public void exportApp(@PathVariable String app) {
        logger.trace("exportApp: {}", app);
    }

    @ResponseBody
    @RequestMapping(value = "/apps/{app}", method = RequestMethod.POST)
    public void importApp(@PathVariable String app) {
        logger.trace("importApp: {}", app);
    }

    @ResponseBody
    @RequestMapping(value = "/**")
    public void passthough(HttpServletRequest request, HttpServletResponse response) {
        logger.trace("passthrough - method: {}, path: {}, params: {}", new Object[]{
                request.getMethod(), request.getServletPath(), request.getQueryString()});
        final AsyncContext asyncContext = request.startAsync(request, response);
        try {
            ServletRestRequest restRequest = new ServletRestRequest(request);
            AsyncServletRestChannel channel = new AsyncServletRestChannel(restRequest, asyncContext);
            restController.dispatchRequest(restRequest, channel);
        } catch (IOException e) {
            logger.error("Error processing passthrough request", e);
            response.setStatus(500);
            asyncContext.complete();
        }
    }

    static class AsyncServletRestChannel implements RestChannel {

        private final RestRequest restRequest;
        private final AsyncContext asyncContext;

        AsyncServletRestChannel(RestRequest restRequest, AsyncContext asyncContext) {
            this.restRequest = restRequest;
            this.asyncContext = asyncContext;
        }

        @Override
        public void sendResponse(RestResponse response) {
            HttpServletResponse resp = (HttpServletResponse) asyncContext.getResponse();
            logger.debug("response content type: {}", response.contentType());
            resp.setContentType(response.contentType());
            logger.debug("isBrowser: {}", RestUtils.isBrowser(restRequest.header("User-Agent")));
            if (RestUtils.isBrowser(restRequest.header("User-Agent"))) {
                resp.addHeader("Access-Control-Allow-Origin", "*");
                logger.debug("request method: {}", restRequest.method());
                if (restRequest.method() == RestRequest.Method.OPTIONS) {
                    // also add more access control parameters
                    resp.addHeader("Access-Control-Max-Age", "1728000");
                    resp.addHeader("Access-Control-Allow-Methods", "PUT, DELETE");
                    resp.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
                }
            }
            String opaque = restRequest.header("X-Opaque-Id");
            logger.debug("opaque id: {}", opaque);
            if (opaque != null) {
                resp.addHeader("X-Opaque-Id", opaque);
            }
            try {
                int contentLength = response.contentLength() + response.prefixContentLength() + response.suffixContentLength();
                logger.debug("response content length: {}", contentLength);
                resp.setContentLength(contentLength);

                ServletOutputStream out = resp.getOutputStream();
                if (response.prefixContent() != null) {
                    logger.debug("send prefix content of length {}", response.prefixContentLength());
                    out.write(response.prefixContent(), 0, response.prefixContentLength());
                }

                logger.debug("send content of length {}", response.contentLength());
                out.write(response.content(), 0, response.contentLength());

                if (response.suffixContent() != null) {
                    logger.debug("send suffix content of length {}", response.suffixContentLength());
                    out.write(response.suffixContent(), 0, response.suffixContentLength());
                }

                out.close();
            } catch (IOException e) {
                logger.error("error sending response", e);
                resp.setStatus(500);
            } finally {
                asyncContext.complete();
            }
        }
    }

}

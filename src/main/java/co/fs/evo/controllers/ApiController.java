package co.fs.evo.controllers;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.support.RestUtils;
import org.json.simple.JSONObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;

import co.fs.evo.exceptions.EvoException;
import co.fs.evo.apps.resources.ResourceCache;
import co.fs.evo.http.rest.ServletRestRequest;
import co.fs.evo.services.SearchService;

@Controller
@RequestMapping("/v1")
public class ApiController {

    private static final XLogger logger = XLoggerFactory.getXLogger(ApiController.class);

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ResourceCache resourceCache;
    
    protected RestController restController;

    @PostConstruct
    public void init() {
        restController = ((InternalNode) searchService.getNode()).injector().getInstance(RestController.class);
    }

    @ResponseBody
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/apps/{app}", method = RequestMethod.GET)
    public void exportApp(@PathVariable String app, HttpServletRequest request, HttpServletResponse response) {
        logger.entry(app);
        Map<String, String[]> exportMappings = null;
        String[] mappings = request.getParameterValues("mapping");

        logger.debug("mappings: {}", (Object)mappings);
        if (mappings != null) {
            exportMappings = new HashMap<String, String[]>();
            for (String mapping : mappings) {
                logger.debug("mapping: {}", mapping);
                int sep = mapping.indexOf(":");
                logger.debug("sep idx: {}", sep);
                if (sep != -1) {
                    String idxName = mapping.substring(0, sep);
                    String idxTypes = mapping.substring(sep + 1, mapping.length());
                    logger.debug("idxName:{} idxTypes:{}", idxName, idxTypes);
                    exportMappings.put(idxName, idxTypes.split(","));
                } else {
                    exportMappings.put(mapping, null);
                }
            }
        }

        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            boolean hasApp = searchService.hasApp(app);

            logger.debug("hasApp: {}", hasApp);
            if (hasApp) {
                response.setHeader("Content-disposition", "attachment; filename=" + app + ".zip");
                response.setContentType("application/octet-stream");
                searchService.exportApp(app, out, exportMappings);
            } else {
                JSONObject resp = new JSONObject();
                resp.put("status", "error");
                resp.put("response", "Application not found: " + app);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                Writer writer = new OutputStreamWriter(out);
                resp.writeJSONString(writer);
                writer.flush();
                writer.close();
            }
        } catch (Exception e) {
            logger.error("Error exporting application: " + app);
            logger.debug("Exception:", e);
        } finally {
            logger.debug("Closing output stream");
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.debug("Error closing output stream", e);
                }
            }
        }
        
        logger.exit();
    }

    @ResponseBody
    @RequestMapping(value = "/apps/{app}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> importApp(@PathVariable String app,
            @RequestParam(value = "force", defaultValue = "false") boolean force,
            @RequestParam(value = "mappings", defaultValue = "true") boolean mappings,
            @RequestPart(value = "app", required = false) Part data) {
        logger.entry(app, force, mappings);
        Map<String, Object> resp = new HashMap<String, Object>();

        try {
            if (data == null) {
                logger.debug("no input data found");
                throw new EvoException("No application file found");
            }

            // expire any cached items that might exist for the app
            resourceCache.evict(app);
            
            // import the app
            searchService.importApp(app, data.getInputStream(), force, mappings);

            resp.put("status", "ok");
            resp.put("response", "Application successfully imported: " + app);
        } catch (Exception e) {
            logger.warn("Error importing application: {}", e.getMessage());
            resp.put("status", "error");
            resp.put("response", e.getMessage());
        }

        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/**")
    public void passthough(HttpServletRequest request, HttpServletResponse response) {
        logger.entry();
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
        logger.exit();
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
                int contentLength = response.contentLength()
                        + (response.prefixContentLength() != -1 ? response.prefixContentLength() : 0)
                        + (response.suffixContentLength() != -1 ? response.suffixContentLength() : 0);
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

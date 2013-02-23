package co.fs.evo.http.response;

import static co.fs.evo.Constants.MILLISECONDS_IN_YEAR;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.tika.io.IOUtils;
import org.elasticsearch.action.get.GetResponse;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.hazelcast.spring.context.SpringAware;

import co.fs.evo.common.StringUtils;
import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotAllowedException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.http.request.RequestInfo;
import co.fs.evo.security.EvoUser;
import co.fs.evo.services.SearchService;

@Component
@SpringAware
@Scope("prototype")
public class StaticResourceResponse implements Response {

	private static final long serialVersionUID = 5228874394955082371L;
    private static final XLogger logger = XLoggerFactory.getXLogger(StaticResourceResponse.class);
    
    @Autowired
    protected transient SearchService searchService;

    // serializable data
    protected String mime;
    protected byte[] data = null;
    protected long lastModified;
    
    protected void sendCacheHeaders(HttpServletResponse response) {
        logger.entry();
        // Build an expiration date 1 year from now. This is the max duration according to RFC guidelines
        long expiration = System.currentTimeMillis() +  MILLISECONDS_IN_YEAR;
        logger.debug("expiration: {}", expiration);

        try {
	        // set cache headers
	        logger.debug("setting cache control headers");
	        response.addHeader("Cache-Control", "max-age=31556926, public"); // 1 year
	        response.addDateHeader("Expires", expiration);
	        response.addDateHeader("Last-Modified", lastModified);
	        
        } catch (Exception e) {
        	logger.debug("Error set cache headers: {}", e);
        }
        logger.exit();
        
    }

    protected boolean checkIfModified(RequestInfo request) {
        logger.entry();
        boolean modified = true;
        try {
            // does the client have a cached copy
        	long modifiedSince = request.getModifiedSince();

            if (modifiedSince != -1) {
                logger.debug("sinceModified: {}", modifiedSince);

                // does the client cache reflect the latest version
                if (lastModified <= modifiedSince) {
                    logger.debug("resource not modified");
                    modified = false;
                }
            }
        } catch (Exception e) {
            // log error and do nothing, will default to resource being considered modified
            logger.debug("Error checking if static resoruce modified", e);
        }

        logger.exit();
        return modified;
    }

	@Override
	public void send(RequestInfo request, AsyncContext ctx, EvoUser userDetails)
			throws ResourceException {

        logger.entry();

        HttpServletResponse response = (HttpServletResponse)ctx.getResponse();
        
        // static resources only support HTTP GET
        response.setHeader("Allow", "GET");

        // throw error if this is not a GET request
        boolean isGet = "GET".equals(request.getMethod());
        logger.debug("isGet: {}", isGet);
        if (!isGet) {
            logger.debug("method not allowed for static resource: {}", request.getMethod());
            throw new NotAllowedException("Static resoruce only support GET requests, received: " + request.getMethod());
        }

        // send the cache expiration headers
        sendCacheHeaders(response);

        // check if the browser has a cached copy of the resource
        boolean modified = checkIfModified(request);
        if (modified) {
            logger.debug("resource modified");
            try {
                OutputStream out = new BufferedOutputStream(response.getOutputStream());

                response.setContentType(mime);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentLength(data.length);

                IOUtils.write(data, out);
                IOUtils.closeQuietly(out);
            } catch (IOException e) {
                logger.debug("Error writing response");
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
            logger.debug("resource not modified");

            // tell browser to use cached copy
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
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

        // get the resource source
        Map<String, Object> source = response.sourceAsMap();

        // calculate the last modifed date based on the timestamp field returned with the 
        // doc response. We need to truncate the milliseconds portion.
        lastModified = (long)response.field("_timestamp").value()/1000*1000;
        logger.debug("lastModified: {}", lastModified);

        mime = (String) source.get("mime");
        logger.debug("mime: {}", mime);

        // get code and convert to bytes for response
        String code = (String) source.get("code");
        try {
            if (mime.startsWith("image")) {
                logger.trace("decoding base64 image");
                data = StringUtils.decodeBase64(code);
            } else {
                logger.trace("getting string as utf-8 bytes");
                data = code.getBytes("UTF-8");
            }
        } catch (Exception e) {
            logger.debug("Error processing static resource", e);
            throw new InternalErrorException("Error processing static resource", e);
        }

        logger.exit();		
	}

	@Override
	public void readData(DataInput in) throws IOException {
        mime = in.readUTF();
        lastModified = in.readLong();
        int len = in.readInt();
        data = new byte[len];
        in.readFully(data, 0, len);
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
        out.writeUTF(mime);
        out.writeLong(lastModified);
        out.writeInt(data.length);
        out.write(data);
	}

}

package co.fs.evo.apps.resources;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import com.hazelcast.spring.context.SpringAware;

import org.apache.commons.codec.binary.Base64;
import org.apache.tika.io.IOUtils;
import org.elasticsearch.action.get.GetResponse;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotAllowedException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.javascript.RequestInfo;
import co.fs.evo.security.EvoUser;
import static co.fs.evo.Constants.*;

/**
 * Represents a static resources (html, css, js, image)
 * 
 */
@Component
@SpringAware
@Scope("prototype")
public class StaticResource extends Resource {

    private static final long serialVersionUID = 6329914850918878864L;
    private static final XLogger logger = XLoggerFactory.getXLogger(StaticResource.class);

    // serializable data
    protected String mime;
    protected byte[] data = null;
    protected long lastModified;

    /**
     * Configures the resource. Get data from app index, get last modified date, and gets the response bytes.
     * 
     * @throws ResourceException when there is an error processing the resource
     */
    @Override
    public void loadFromDisk(String app, String dir, String resource) throws ResourceException {
        super.loadFromDisk(app, dir, resource);
        logger.entry();

        // get the resource doc from the app index
        GetResponse doc = getDoc(new String[]{"_timestamp", "_source"});

        // get the resource source
        Map<String, Object> source = doc.sourceAsMap();

        // calculate the last modifed date based on the timestamp field returned with the 
        // doc response. We need to truncate the milliseconds portion.
        lastModified = (long)doc.field("_timestamp").value()/1000*1000;
        logger.debug("lastModified: {}", lastModified);

        // set the mime
        mime = (String) source.get("mime");
        logger.debug("mime: {}", mime);

        // get code and convert to bytes for response
        String code = (String) source.get("code");
        try {
            if (mime.startsWith("image")) {
                logger.debug("decoding base64 image");
                data = Base64.decodeBase64(code);
            } else {
                logger.debug("getting string as utf-8 bytes");
                data = code.getBytes("UTF-8");
            }
        } catch (Exception e) {
            logger.debug("Error processing static resource", e);
            throw new InternalErrorException("Error processing static resource", e);
        }

        logger.exit();
    }

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

    /*
     * (non-Javadoc)
     * @see co.fs.evo.apps.resources.Resource#process(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession)
     */
    @Override
    public void process(RequestInfo request, AsyncContext ctx, EvoUser userDetails) throws ResourceException {
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

    /*
     * (non-Javadoc)
     * @see co.fs.evo.apps.resources.Resource#readData(java.io.DataInput)
     */
    @Override
    public void readData(DataInput in) throws IOException {
        super.readData(in);

        // unserialize mime, lastModifeid, and data
        mime = in.readUTF();

        // create date from long
        lastModified = in.readLong();

        // read length of data, create array and read the data
        int len = in.readInt();
        data = new byte[len];
        in.readFully(data, 0, len);
    }

    /*
     * (non-Javadoc)
     * @see co.fs.evo.apps.resources.Resource#writeData(java.io.DataOutput)
     */
    @Override
    public void writeData(DataOutput out) throws IOException {
        super.writeData(out);

        // we only need to serialize the data that will be re-used in process
        // mime, lastModified, and data
        out.writeUTF(mime);

        // the date can be represented as a long
        out.writeLong(lastModified);

        // write length of data so we know how much to read when unserializing
        out.writeInt(data.length);
        out.write(data);
    }

}

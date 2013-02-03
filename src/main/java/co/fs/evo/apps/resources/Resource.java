package co.fs.evo.apps.resources;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.servlet.AsyncContext;

import com.hazelcast.nio.DataSerializable;
import com.hazelcast.spring.context.SpringAware;

import org.elasticsearch.action.get.GetResponse;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import co.fs.evo.exceptions.EvoException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.javascript.RequestInfo;
import co.fs.evo.security.EvoUser;
import co.fs.evo.services.SearchService;

@SpringAware
public abstract class Resource implements DataSerializable {

    private static final long serialVersionUID = -316985492511753285L;
    private static final XLogger logger = XLoggerFactory.getXLogger(Resource.class);

    @Autowired
    protected transient SearchService searchService;

    protected String app;
    protected String dir;
    protected String resource;

    public void loadFromDisk(String app, String dir, String resource) throws ResourceException {
        logger.entry(app, dir, resource);
        this.app = app;
        this.dir = dir;
        this.resource = resource;
        logger.exit();
    }

    /**
     * Processes the resource request
     * 
     * @param request the http request for the resource
     * @param response the http response for the resource
     * @param session the http session for the resource
     */
    public abstract void process(RequestInfo request, AsyncContext ctx, EvoUser userDetails)
            throws ResourceException;

    /**
     * For serialization in hazelcast
     * 
     * @see com.hazelcast.nio.DataSerializable#readData(java.io.DataInput)
     */
    public void readData(DataInput in) throws IOException {
        app = in.readUTF();
        dir = in.readUTF();
        resource = in.readUTF();
    }

    /**
     * For serialization in hazelcast
     * 
     * @see com.hazelcast.nio.DataSerializable#writeData(java.io.DataOutput)
     */
    public void writeData(DataOutput out) throws IOException {
        out.writeUTF(app);
        out.writeUTF(dir);
        out.writeUTF(resource);
    }

    /**
     * @return the app
     */
    public String getApp() {
        return app;
    }

    /**
     * @return the dir
     */
    public String getDir() {
        return dir;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * Gets the resource document
     * 
     * @param app the app the resources belongs to
     * @param dir the type of resource
     * @param resource the name/id of the resource
     * @param fields what fields you want returned
     * @return the document
     * @throws EvoException if resource document is not found
     */
    protected GetResponse getDoc(String[] fields) throws NotFoundException {
        logger.entry((Object) fields);
        logger.debug("searchService: {}", searchService);
        logger.debug("app:{}, dir:{}, resource:{}", new Object[]{app, dir, resource});
        GetResponse doc = searchService.getAppResource(app, dir, resource, fields);
        logger.debug("doc: {}", doc);
        if (doc == null || !doc.exists()) {
            throw new NotFoundException("Resource not found: " + resource);
        }

        logger.exit();
        return doc;
    }

}

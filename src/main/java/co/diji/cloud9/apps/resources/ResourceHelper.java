package co.diji.cloud9.apps.resources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import co.diji.cloud9.exceptions.resources.ResourceException;

@Component
public class ResourceHelper {

    protected static final Logger logger = LoggerFactory.getLogger(ResourceHelper.class);

    private static final Set<String> STATIC_RESOURCES = new HashSet<String>(Arrays.asList(new String[]{
            "css", "images", "js", "html"}));

    @Autowired
    private ApplicationContext appContext;

    public Resource getResource(String app, String dir, String resource) throws ResourceException {
        logger.trace("in getResource app:{} dir:{} resource:{}", new Object[]{app, dir, resource});
        Resource r;

        if (dir == null && resource == null) {
            // only given app name, go to index html by default
            logger.debug("no dir or resource, using html/index.html");
            dir = "html";
            resource = "index.html";
        } else if (resource == null) {
            // only app name and resource name which spring thinks is actually the dir
            // set the resource name and dir to html
            logger.debug("no resource, using html/{}", dir);
            resource = dir;
            dir = "html";
        }

        // if this is a static resource
        boolean isStatic = STATIC_RESOURCES.contains(dir);
        logger.debug("isStatic: {}", isStatic);
        if (isStatic) {
            r = appContext.getBean(StaticResource.class);
        } else {
            // javascript controllers are in the "controllers" type/dir and have resource name of dir + .js
            logger.debug("found javascript controller, using controllers/{}.js", dir);
            resource = dir + ".js";
            dir = "controllers";
            r = appContext.getBean(JavascriptResource.class);
        }

        // run resource setup code.
        r.setup(app, dir, resource);

        logger.trace("exit getResource");
        return r;
    }

}

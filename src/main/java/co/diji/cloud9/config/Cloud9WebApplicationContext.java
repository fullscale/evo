package co.diji.cloud9.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class Cloud9WebApplicationContext extends XmlWebApplicationContext {

    private static final Logger logger = LoggerFactory.getLogger(Cloud9WebApplicationContext.class);
    private static final String securityResourceVar = "#security.context#";
    private static final String defaultSecurityContext = "/WEB-INF/spring/security-context.xml";

    @Override
    public Resource getResource(String location) {
        logger.trace("in getResource location:{}", location);
        Resource resource = null;
        if (location.equals(securityResourceVar)) {
            String userSecurityFile = System.getProperty("c9.security.context", null);
            logger.debug("userSecurityFile: {}", userSecurityFile);
            if (userSecurityFile != null) {
                resource = super.getResource("file:" + userSecurityFile);
            } else {
                resource = super.getResource(defaultSecurityContext);
            }
        } else {
            resource = super.getResource(location);
        }

        logger.trace("exit getResource: {}", resource);
        return resource;
    }
}

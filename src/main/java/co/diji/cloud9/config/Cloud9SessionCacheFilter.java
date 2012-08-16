package co.diji.cloud9.config;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import com.hazelcast.web.SessionListener;
import com.hazelcast.web.WebFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;

public class Cloud9SessionCacheFilter implements WebApplicationInitializer {

    private static final Logger logger = LoggerFactory.getLogger(Cloud9SessionCacheFilter.class);
    private EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST);

    @Override
    public void onStartup(ServletContext context) {
        logger.trace("in onStartup");
        Boolean enableSessionCache = "enable".equals(System.getProperty("c9.session.cache", "false"));
        logger.debug("enableSessionCache: {}", enableSessionCache);
        if (enableSessionCache == true) {
            FilterRegistration cacheFilter = context.addFilter("hazelcast-filter", new WebFilter());
            cacheFilter.setInitParameter("instance-name", "cloud9");
            cacheFilter.setInitParameter("map-name", "session-cache");
            cacheFilter.setInitParameter("sticky-session", "false");
            cacheFilter.setInitParameter("cookie-name", "cloud9-sid");
            cacheFilter.setInitParameter("debug", "true");
            cacheFilter.setInitParameter("shutdown-on-destroy", "false");

            cacheFilter.addMappingForUrlPatterns(dispatches, false, "/*");
            context.addListener(new SessionListener());
        }
    }
}

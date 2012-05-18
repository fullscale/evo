package co.diji.cloud9.config;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.springframework.web.WebApplicationInitializer;

import com.hazelcast.web.SessionListener;
import com.hazelcast.web.WebFilter;

public class Cloud9SessionCacheFilter implements WebApplicationInitializer {

    private EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST);

    @Override
    public void onStartup(ServletContext context) {

        FilterRegistration cacheFilter = context.addFilter("hazelcast-filter", new WebFilter());
        cacheFilter.setInitParameter("map-name", "session-cache");
        cacheFilter.setInitParameter("sticky-session", "false");
        cacheFilter.setInitParameter("cookie-name", "cloud9-sid");
        cacheFilter.setInitParameter("debug", "true");

        cacheFilter.addMappingForUrlPatterns(dispatches, false, "/*");
        context.addListener(new SessionListener());

    }
}

package co.diji.cloud9.http;

import javax.servlet.http.HttpSessionEvent;

import com.hazelcast.web.SessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorSuppressingSessionListener extends SessionListener {
    private static final Logger logger = LoggerFactory.getLogger(ErrorSuppressingSessionListener.class);

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        try {
            super.sessionDestroyed(httpSessionEvent);
        } catch (Exception e) {
            logger.debug("Error destroying session", e);
        }
    }
}
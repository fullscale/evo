package co.fs.evo.http;

import javax.servlet.http.HttpSessionEvent;

import com.hazelcast.web.SessionListener;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class ErrorSuppressingSessionListener extends SessionListener {
    private static final XLogger logger = XLoggerFactory.getXLogger(ErrorSuppressingSessionListener.class);

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        try {
            super.sessionDestroyed(httpSessionEvent);
        } catch (Exception e) {
            logger.debug("Error destroying session", e);
        }
    }
}
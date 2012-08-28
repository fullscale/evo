package co.diji.cloud9;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import co.diji.cloud9.config.AppContext;
import co.diji.cloud9.config.RootContext;
import co.diji.cloud9.http.ErrorSuppressingSessionListener;

public final class Cloud9 {

    private static final XLogger logger = XLoggerFactory.getXLogger(Cloud9.class);

    public static void main(String[] args) throws Exception {
        logger.entry();

        // read user specified properties
        int httpPort = Integer.parseInt(System.getProperty("c9.http.port", "2600"));
        int httpsPort = Integer.parseInt(System.getProperty("c9.https.port", "2643"));
        Boolean forceHttps = "force".equals(System.getProperty("c9.https.enable", "false"));

        logger.debug("httpPort: {}", httpPort);
        logger.debug("httpsPort:{}", httpsPort);
        logger.debug("forceHttps: {}", forceHttps);

        Server server = new Server(httpPort);

        // TODO fix once running war-less
        String tmpDir = ".";

        // setup an SSL socket
        // get the keystore/keypass otherwise use cloud9 default
        String keypass = System.getProperty("c9.https.keypass", "3f038de0-6606-11e1-b86c-0800200c9a66");
        String keystore = System.getProperty("c9.https.keystore", tmpDir + "/etc/security/c9.default.keystore");

        // create a secure channel
        final SslContextFactory sslContextFactory = new SslContextFactory(keystore);
        sslContextFactory.setKeyStorePassword(keypass);
        sslContextFactory.setKeyManagerPassword(keypass);

        final SslSocketConnector sslConn = new SslSocketConnector(sslContextFactory);
        sslConn.setPort(httpsPort);

        // we're not forcing HTTPS so start an HTTP channel as well
        if (!forceHttps) {
            logger.info("Enabling SSL on port {}", httpsPort);
            SelectChannelConnector selectChannelConnector = new SelectChannelConnector();
            selectChannelConnector.setPort(httpPort);
            server.setConnectors(new Connector[]{sslConn, selectChannelConnector});
        } else {
            logger.info("Enforcing SSL on port {}", httpsPort);
            server.setConnectors(new Connector[]{sslConn});
        }

        // main servlet context
        final ServletContextHandler context = new ServletContextHandler(server, "/", true, false);

        // setup spring contexts
        AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
        root.register(RootContext.class);
        context.addEventListener(new ContextLoaderListener(root));

        AnnotationConfigWebApplicationContext dispatch = new AnnotationConfigWebApplicationContext();
        dispatch.register(AppContext.class);

        // setup hazelcast filter
        // spring delegating filter proxy which delegates to our hazelcastWebFilter bean
        DelegatingFilterProxy hazelcastFilter = new DelegatingFilterProxy("hazelcastWebFilter");
        hazelcastFilter.setTargetFilterLifecycle(true);
        context.addFilter(new FilterHolder(hazelcastFilter), "/*", // send all requests though the filter
                EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST));

        // our custom hazelcast event listener that swallows shutdown exceptions
        context.addEventListener(new ErrorSuppressingSessionListener());

        // setup spring security filters
        DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy("springSecurityFilterChain");
        context.addFilter(new FilterHolder(springSecurityFilter), "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

        // setup dispatcher servlet
        DispatcherServlet dispatcher = new DispatcherServlet(dispatch);
        dispatcher.setDispatchOptionsRequest(true);

        // servlet holder, initOrder is same as load-on-startup in web.xml
        final ServletHolder servletHolder = new ServletHolder(dispatcher);
        servletHolder.setInitOrder(1);

        // session timeout configuration
        context.getSessionHandler().getSessionManager().setMaxInactiveInterval(43200); // 12 hours
        context.addServlet(servletHolder, "/");
        server.setStopAtShutdown(true);
        server.setHandler(context);

        logger.debug("starting jetty");
        server.start();
        server.join();
        logger.exit();
    }

}

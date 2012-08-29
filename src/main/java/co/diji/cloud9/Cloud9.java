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
import org.eclipse.jetty.util.thread.QueuedThreadPool;
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
        logger.debug("creating context");
        final ServletContextHandler context = new ServletContextHandler(server, "/", true, false);

        // setup spring contexts
        logger.debug("create root app context");
        AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
        logger.debug("registering root context");
        root.register(RootContext.class);
        logger.debug("adding root context listener");
        context.addEventListener(new ContextLoaderListener(root));

        logger.debug("create app context");
        AnnotationConfigWebApplicationContext dispatch = new AnnotationConfigWebApplicationContext();
        logger.debug("registering app context");
        dispatch.register(AppContext.class);

        // setup hazelcast filter
        // spring delegating filter proxy which delegates to our hazelcastWebFilter bean
        logger.debug("creating hazelcast filter");
        DelegatingFilterProxy hazelcastFilter = new DelegatingFilterProxy("hazelcastWebFilter");
        hazelcastFilter.setTargetFilterLifecycle(true);
        logger.debug("adding hazelcast filter to servlet context");
        context.addFilter(new FilterHolder(hazelcastFilter), "/*", // send all requests though the filter
                EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST));

        // our custom hazelcast event listener that swallows shutdown exceptions
        logger.debug("adding hazelcast event listener");
        context.addEventListener(new ErrorSuppressingSessionListener());

        // setup spring security filters
        logger.debug("creating spring security filter");
        DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy("springSecurityFilterChain");
        logger.debug("adding spring security filter to servlet context");
        context.addFilter(new FilterHolder(springSecurityFilter), "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

        // setup dispatcher servlet
        logger.debug("creating dispatcher");
        DispatcherServlet dispatcher = new DispatcherServlet(dispatch);
        dispatcher.setDispatchOptionsRequest(true);

        // servlet holder, initOrder is same as load-on-startup in web.xml
        logger.debug("creating servlet holder for dispatcher");
        final ServletHolder servletHolder = new ServletHolder(dispatcher);
        servletHolder.setInitOrder(1);

        // session timeout configuration
        logger.debug("setting session timeout");
        context.getSessionHandler().getSessionManager().setMaxInactiveInterval(43200); // 12 hours
        
        // register our spring dispatcher servlet
        logger.debug("registering dispatcher servlet");
        context.addServlet(servletHolder, "/");
        
        // set the directory where our resources are located
        logger.debug("for resources");
        context.setResourceBase("resources");
        
        // configure our connection thread pool
        logger.debug("create jetty thread pool");
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(500);
        server.setThreadPool(threadPool);
        logger.debug("threads: {}", server.getThreadPool().getThreads());
        
        logger.debug("enabling stop on shutdown");
        server.setStopAtShutdown(true);
        
        logger.debug("setting jetty handler to the servlet context");
        server.setHandler(context);

        logger.debug("starting jetty");
        server.start();
        
        logger.debug("jetty started");
        server.join();
        
        logger.debug("jetty stopped");
        logger.exit();
    }

}

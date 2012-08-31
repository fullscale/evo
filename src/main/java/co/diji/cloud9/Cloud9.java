package co.diji.cloud9;

import java.io.File;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import ch.qos.logback.access.jetty.RequestLogImpl;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
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
import co.diji.cloud9.services.ConfigService;

public final class Cloud9 {

    private static final XLogger logger = XLoggerFactory.getXLogger(Cloud9.class);

    public static void main(String[] args) throws Exception {
        logger.entry();
        logger.info("Starting Cloud9");
        
        // get the config service bean from root context
        ConfigService config = ConfigService.getConfigService();

        // create jetty server
        Server server = new Server();

        // Setup http connector
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(config.getHttpPort());
        connector.setMaxIdleTime(30000);
        connector.setStatsOn(false);
        server.setConnectors(new Connector[]{connector});

        // see if we need to enabled https
        if (config.getHttpsEnabled()) {
            logger.info("HTTPS enabled");
            final SslContextFactory sslContextFactory = new SslContextFactory(config.getHttpsKeystore());
            sslContextFactory.setKeyStorePassword(config.getHttpsKeypass());
            sslContextFactory.setKeyManagerPassword(config.getHttpsKeypass());

            final SslSocketConnector sslConn = new SslSocketConnector(sslContextFactory);
            sslConn.setPort(config.getHttpsPort());
            sslConn.setStatsOn(false);
            server.addConnector(sslConn);
            sslConn.open();
        }

        // main servlet context
        logger.debug("creating context");
        final ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", true, false);

        // setup root spring context
        logger.debug("create root app context");
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        logger.debug("registering root context");
        rootContext.register(RootContext.class);

        // add spring root context listener
        logger.debug("adding root context listener");
        servletContextHandler.addEventListener(new ContextLoaderListener(rootContext));

        // setup spring app context
        logger.debug("create app context");
        AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
        logger.debug("registering app context");
        appContext.register(AppContext.class);

        // setup hazelcast filter
        // spring delegating filter proxy which delegates to our hazelcastWebFilter bean
        logger.debug("creating hazelcast filter");
        DelegatingFilterProxy hazelcastFilter = new DelegatingFilterProxy("hazelcastWebFilter");
        hazelcastFilter.setTargetFilterLifecycle(true);
        logger.debug("adding hazelcast filter to servlet context");
        servletContextHandler.addFilter(new FilterHolder(hazelcastFilter), "/*", // send all requests though the filter
                EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST));

        // our custom hazelcast event listener that swallows shutdown exceptions
        logger.debug("adding hazelcast event listener");
        servletContextHandler.addEventListener(new ErrorSuppressingSessionListener());

        // setup spring security filters
        logger.debug("creating spring security filter");
        DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy("springSecurityFilterChain");
        logger.debug("adding spring security filter to servlet context");
        servletContextHandler.addFilter(new FilterHolder(springSecurityFilter),
                "/*",
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

        // setup dispatcher servlet
        logger.debug("creating dispatcher");
        DispatcherServlet dispatcher = new DispatcherServlet(appContext);
        dispatcher.setDispatchOptionsRequest(true);

        // servlet holder, initOrder is same as load-on-startup in web.xml
        logger.debug("creating servlet holder for dispatcher");
        final ServletHolder servletHolder = new ServletHolder(dispatcher);
        servletHolder.setInitOrder(1);

        // session timeout configuration
        logger.debug("setting session timeout");
        servletContextHandler.getSessionHandler().getSessionManager().setMaxInactiveInterval(43200); // 12 hours

        // register our spring dispatcher servlet
        logger.debug("registering dispatcher servlet");
        servletContextHandler.addServlet(servletHolder, "/");

        // set the directory where our resources are located
        logger.debug("for resources");
        servletContextHandler.setResourceBase("resources");

        // configure our connection thread pool
        logger.debug("create jetty thread pool");
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(500);
        server.setThreadPool(threadPool);
        logger.debug("threads: {}", server.getThreadPool().getThreads());

        logger.debug("enabling stop on shutdown");
        server.setStopAtShutdown(true);

        // create request log handler
        logger.debug("Enabling request log handler");
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        RequestLogImpl requestLog = new RequestLogImpl();
        requestLog.setQuiet(true);
        requestLog.setFileName(config.getHome() + File.separator + "etc" + File.separator + "logback-access.xml");
        requestLogHandler.setRequestLog(requestLog);

        // register out handlers with jetty
        logger.debug("Creating handler collection with servlet and request log handlers");
        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{servletContextHandler, requestLogHandler});

        logger.debug("setting jetty handler to the handler collection");
        server.setHandler(handlers);

        logger.debug("starting jetty");
        server.start();

        logger.debug("jetty started");
        logger.info("Cloud9 started");
        server.join();

        logger.debug("jetty stopped");
        logger.exit();
    }

}

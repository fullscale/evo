package co.diji.cloud9;

import java.io.File;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import ch.qos.logback.access.jetty.RequestLogImpl;

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.spdy.server.NPNServerConnectionFactory;
import org.eclipse.jetty.spdy.server.SPDYServerConnectionFactory;
import org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnectionFactory;
import org.eclipse.jetty.spdy.server.http.PushStrategy;
import org.eclipse.jetty.spdy.server.http.ReferrerPushStrategy;

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
        logger.info("Starting EVO");

        // get the config service bean from root context
        ConfigService config = ConfigService.getConfigService();
        
        // configure our connection thread pool
        logger.debug("create jetty thread pool");
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(config.getHttpMaxThreads());

        // create jetty server
        Server server = new Server(threadPool);
        server.manage(threadPool);
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);

        // HTTP configuration
        HttpConfiguration httpConf = new HttpConfiguration();
        httpConf.setSecurePort(config.getHttpsPort());
        httpConf.addCustomizer(new ForwardedRequestCustomizer());
        httpConf.addCustomizer(new SecureRequestCustomizer());
        
        // HTTP connector
        HttpConnectionFactory http = new HttpConnectionFactory(httpConf);
        ServerConnector httpConnector = new ServerConnector(server, http);
        httpConnector.setPort(2600);
        httpConnector.setIdleTimeout(10000);
        server.addConnector(httpConnector);
        
        // SSL context
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(config.getHttpsKeystore());
        sslContextFactory.setKeyStorePassword(config.getHttpsKeypass());
        sslContextFactory.setKeyManagerPassword(config.getHttpsKeypass());
        sslContextFactory.setTrustStorePath(config.getHttpsKeystore());
        sslContextFactory.setExcludeCipherSuites(
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        
        // see if we need to enabled https
        if (config.getHttpsEnabled()) {
        	
        	if (config.getSpdyEnabled()) {
        		
        		logger.info("HTTPS/SPDY enabled");
        		
        		/* Spdy Connector */
        		SPDYServerConnectionFactory.checkNPNAvailable();

	            PushStrategy push = new ReferrerPushStrategy();
	            HTTPSPDYServerConnectionFactory spdy2 = new HTTPSPDYServerConnectionFactory(2, httpConf, push);
	            spdy2.setInputBufferSize(8192);
	            spdy2.setInitialWindowSize(32768);
	
	            HTTPSPDYServerConnectionFactory spdy3 = new HTTPSPDYServerConnectionFactory(3, httpConf,push);
	            spdy2.setInputBufferSize(8192);
	
	            NPNServerConnectionFactory npn = new NPNServerConnectionFactory(spdy3.getProtocol(), spdy2.getProtocol(), http.getProtocol());
	            npn.setDefaultProtocol(http.getProtocol());
	            npn.setInputBufferSize(1024);
	            
	            SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, npn.getProtocol());
	            ServerConnector sslConnector = new ServerConnector(server, ssl, npn, spdy3, spdy2, http);
	            sslConnector.setPort(config.getHttpsPort());
	            server.addConnector(sslConnector);
	            
        	} else {
            	logger.info("HTTPS enabled");
            	
	            SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, "http/1.1");
	            ServerConnector sslConnector = new ServerConnector(server, ssl, http);
	            sslConnector.setPort(config.getHttpsPort());
	            server.addConnector(sslConnector);
        	}
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

        // setup hazelcast filter if hazelcast and session caching are enabled
        if (config.getHazelcastEnabled() && config.getSessionCacheEnabled()) {
            // spring delegating filter proxy which delegates to our hazelcastWebFilter bean
            logger.debug("Enabling session cache");
            logger.debug("creating hazelcast filter");
            DelegatingFilterProxy hazelcastFilter = new DelegatingFilterProxy("hazelcastWebFilter");
            hazelcastFilter.setTargetFilterLifecycle(true);
            logger.debug("adding hazelcast filter to servlet context");
            servletContextHandler.addFilter(new FilterHolder(hazelcastFilter), "/*", // send all requests though the filter
                    EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST));

            // our custom hazelcast event listener that swallows shutdown exceptions
            logger.debug("adding hazelcast event listener");
            servletContextHandler.addEventListener(new ErrorSuppressingSessionListener());
        } else {
            logger.info("Session cache disabled");
        }

        // setup spring security filters
        logger.debug("creating spring security filter");
        DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy("springSecurityFilterChain");
        logger.debug("adding spring security filter to servlet context");
        servletContextHandler.addFilter(new FilterHolder(springSecurityFilter),
                "/*",
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
        
        // setup gzip filter
        if (config.getGzipEnabled()) {
        	servletContextHandler.addFilter(GzipFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        }

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
        servletContextHandler.getSessionHandler().getSessionManager().setMaxInactiveInterval(config.getHttpSessionTimeout());

        // register our spring dispatcher servlet
        logger.debug("registering dispatcher servlet");
        servletContextHandler.addServlet(servletHolder, "/");

        // set the directory where our resources are located
        logger.debug("for resources");
        servletContextHandler.setResourceBase("resources");

        logger.debug("enabling stop on shutdown");
        server.setStopAtShutdown(true);

        // create request log handler if enabled
        if (config.getHttpRequestLogEnabled()) {
            logger.info("Enabling request log");
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
        } else {
            logger.debug("setting jetty handler to servlet handler");
            server.setHandler(servletContextHandler);
        }

        logger.debug("starting jetty");
        server.start();

        logger.debug("jetty started");
        logger.info("EVO started");
        server.join();

        logger.debug("jetty stopped");
        logger.exit();
    }

}

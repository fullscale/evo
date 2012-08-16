package co.diji.cloud9;

import java.io.File;
import java.net.URL;
import java.security.ProtectionDomain;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.fuin.utils4j.Utils4J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Cloud9 {

    private static final Logger logger = LoggerFactory.getLogger(Cloud9.class);

    public static void main(String[] args) throws Exception {

        // read user specified properties
        int httpPort = Integer.parseInt(System.getProperty("c9.http.port", "2600"));

        int httpsPort = Integer.parseInt(System.getProperty("c9.https.port", "2643"));

        Boolean forceHttps = "force".equals(System.getProperty("c9.https.enable", "false"));

        String libdir = System.getProperty("c9.libdir", null);

        Server server = new Server(httpPort);

        ProtectionDomain domain = Cloud9.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        // load external jars
        if (libdir == null) {
            // checks default location
            File base = new File(location.getPath());
            libdir = base.getParent() + "/lib";
        }
        loadExternalLibs(libdir);

        WebAppContext webapp = new WebAppContext();

        // create a temporary directory to run from
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        String fileSeparator = System.getProperty("file.separator");
        if (!tmpDirPath.endsWith(fileSeparator)) {
            tmpDirPath = tmpDirPath + fileSeparator;
        }

        File tmpDir = new File(tmpDirPath + "cloud9-" + httpPort);

        // if it already exists delete it
        if (tmpDir.exists()) {
            deleteDirectory(tmpDir);
        }

        // create a new empty directory
        if (!(tmpDir.mkdir())) {
            throw new RuntimeException("Unable to create temporary dir: " + tmpDir);
        }

        // this depends on how the JVM was shutdown
        tmpDir.deleteOnExit();
        logger.debug("temp dir: {}", tmpDir);

        // setup an SSL socket

        // get the keystore/keypass otherwise use cloud9 default
        String keypass = System.getProperty("c9.https.keypass", "3f038de0-6606-11e1-b86c-0800200c9a66");
        String keystore = System.getProperty("c9.https.keystore", tmpDir + "/webapp/WEB-INF/security/c9.default.keystore");

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

        webapp.setContextPath("/");
        webapp.setTempDirectory(tmpDir);
        webapp.setWar(location.toExternalForm());
        webapp.setConfigurations(new Configuration[] {
                new WebInfConfiguration(),
                new WebXmlConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new PlusConfiguration(),
                new AnnotationConfiguration(),
                new JettyWebXmlConfiguration() });

        server.setStopAtShutdown(true);
        server.setHandler(webapp);
        server.start();
        server.join();

    }

    /*
     * Loads any external dependencies.
     */
    private static void loadExternalLibs(String directory) {
        try {
            File dir = new File(directory);
            String[] files = dir.list();

            if (files != null) {
                logger.info("Loading external dependencies: {}", directory);
                for (int i = 0; i < files.length; i++) {
                    String fileName = "file:" + directory + "/" + files[i];
                    Utils4J.addToClasspath(fileName);
                }
            }
        } catch (Exception e) {
            logger.warn("Error loading external JAR files");
        }
    }

    /*
     * Really? Java has no way of deleting non-empty directories.
     */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

}

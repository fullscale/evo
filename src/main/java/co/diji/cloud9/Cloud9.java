package co.diji.cloud9;

import java.net.URL;
import java.security.ProtectionDomain;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

public final class Cloud9 {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ProtectionDomain domain = Cloud9.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(location.toExternalForm());
        webapp.setConfigurations(new Configuration[]{new WebInfConfiguration(), new AnnotationConfiguration()});

        server.setStopAtShutdown(true);
        server.setHandler(webapp);
        server.start();
        server.join();
    }
}

package co.diji.cloud9;

import java.net.URL;
import java.security.ProtectionDomain;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public final class Cloud9 {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ProtectionDomain domain = Cloud9.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(location.toExternalForm());

        server.setStopAtShutdown(true);
        server.setHandler(webapp);
        server.start();
        server.join();
    }
}

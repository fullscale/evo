package co.diji.cloud9.config;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
 
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

public class Cloud9Initializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        // Create the 'root' Spring application context
        AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
        root.register(Cloud9Root.class);
 
        // Manages the lifecycle of the root application context
        servletContext.addListener(new ContextLoaderListener(root));

        // Create the 'dispatcher' Spring application context
        AnnotationConfigWebApplicationContext dispatch = new AnnotationConfigWebApplicationContext();
        dispatch.register(Cloud9Dispatcher.class);

        // Register the 'dispatcher' to handle all requests
        ServletRegistration.Dynamic app = servletContext.addServlet("app", new DispatcherServlet(dispatch));
        app.setInitParameter("dispatchOptionsRequest", "true");
        app.setLoadOnStartup(1);
        app.addMapping("/");
    }

}

package co.diji.cloud9.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Controller;

@Configuration
@ComponentScan(basePackages = "co.diji.cloud9", excludeFilters = {@ComponentScan.Filter(Controller.class)})
@ImportResource({"etc/security/security-context.xml"})
public class RootContext {
}
package co.diji.cloud9.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import co.diji.cloud9.services.ConfigService;

@Configuration
@ImportResource({"classpath:security/security-context.xml"})
@ComponentScan(basePackages = {"co.diji.cloud9.apps.resources", "co.diji.cloud9.services", "co.diji.cloud9.javascript"})
public class RootContext {

    @Bean
    public ConfigService configService() {
        return ConfigService.getConfigService();
    }

}
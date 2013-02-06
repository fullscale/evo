package co.fs.evo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import co.fs.evo.services.ConfigService;

@Configuration
@ImportResource({"classpath:security/security-context.xml"})
@ComponentScan(basePackages = {
		"co.fs.evo.apps.resources", 
		"co.fs.evo.services", 
		"co.fs.evo.javascript", 
		"co.fs.evo.http.response"
})

public class RootContext {

    @Bean
    public ConfigService configService() {
        return ConfigService.getConfigService();
    }

}

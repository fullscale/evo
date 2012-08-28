package co.diji.cloud9.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.velocity.VelocityConfig;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "co.diji.cloud9.controllers")
public class AppContext extends WebMvcConfigurerAdapter {

    // for multi-part files
    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    // for velocity support
    @Bean
    public VelocityConfig velocityConfig() {
        VelocityConfigurer conf = new VelocityConfigurer();
        conf.setResourceLoaderPath("classpath:views");
        conf.setOverrideLogging(true);
        return conf;
    }

    @Bean
    public ViewResolver viewResolver() {
        VelocityViewResolver view = new VelocityViewResolver();
        view.setCache(true);
        view.setPrefix("");
        view.setSuffix(".vm");
        view.setExposeSessionAttributes(true);

        return view;
    }

    // for static resources
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:resources");
    }

}
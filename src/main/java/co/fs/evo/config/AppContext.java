package co.fs.evo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.velocity.VelocityConfig;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

import co.fs.evo.services.ConfigService;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "co.fs.evo.controllers")
public class AppContext extends WebMvcConfigurerAdapter {
	
    @Autowired
    protected ConfigService config;

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
    
    // application threadpool
    @Bean
    public TaskExecutor taskExecutor() {
    	ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    	executor.setCorePoolSize(config.getCorePoolSize());
    	executor.setMaxPoolSize(config.getMaxPoolSize());
    	executor.setQueueCapacity(config.getQueueCapacity());
    	executor.setKeepAliveSeconds(config.getKeepaliveSeconds());
    	return executor;	
    }

    // for static resources
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("/resources");
    }
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    	configurer.defaultContentType(MediaType.APPLICATION_JSON);
    	configurer.favorPathExtension(false);
    }
}

package com.lantanagroup.nandina;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@Configuration
@PropertySources({
        @PropertySource(value = { "classpath:config.json" } )
})
public class NandinaApp extends SpringBootServletInitializer implements InitializingBean {
    @Autowired
    private ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(NandinaApp.class, args);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Initialize the config using the Spring application context so that @Value annotations resolve/work
    }
}
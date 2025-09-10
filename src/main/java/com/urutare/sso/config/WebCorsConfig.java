package com.urutare.sso.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig {
    
    @Value( "${app.cors.allowed-origins:http://localhost:2080,http://localhost:3001,http://127.0.0.1:5500}" )
    private String allowedOrigin;
    
    /**
     * CORS configuration for external urls that are allowed.
     *
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer corsConfigurer () {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings (@NotNull CorsRegistry registry) {
                registry.addMapping( "/**" )
                        .allowedOrigins(allowedOrigin)
                        .allowedMethods( "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH" )
                        .allowedHeaders( "*" )
                        .allowCredentials( true );
            }
        };
    }
}
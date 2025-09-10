package com.urutare.sso.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "SSO Service API",
                version = "1.0.0",
                description = "Single Sign-On Service API for authentication and authorization",
                contact = @Contact(
                        name = "SSO Service Team",
                        email = "support@urutare.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:2080", description = "Local Development Server"),
                @Server(url = "https://ia-idp-backend.onrender.com", description = "Production Server")
        }
)
@Configuration
@SecurityScheme(
        name = "bearerAuth", 
        type = SecuritySchemeType.HTTP, 
        bearerFormat = "JWT", 
        scheme = "bearer",
        description = "JWT Bearer token authentication"
)
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI();
    }
}

package com.skynode.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyNode Flight Booking System API")
                        .version("1.0")
                        .description("RESTful API documentation for SkyNode flight management and booking services.")
                        .contact(new Contact()
                                .name("SkyNode Support")
                                .email("support@skynode.com")));
    }
}
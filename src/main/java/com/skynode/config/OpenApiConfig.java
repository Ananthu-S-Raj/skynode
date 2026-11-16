package com.skynode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyNode Flight Booking System API")
                        .version("1.0")
                        .description("SkyNode flight management and booking API. Administrative methods require an ADMIN session. Fetch /api/v1/auth/csrf and preserve its cookie; send the returned header/token with every write, including public bookings. Login uses form-urlencoded email/password at POST /api/v1/auth/login. Fetch a fresh CSRF token after login or logout. GET /api/v1/auth/me reports the current admin state.")
                        .contact(new Contact()
                                .name("SkyNode Support")
                                .email("support@skynode.com")));
    }
}

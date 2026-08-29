package com.softnix.moviebooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI movieBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movie Ticket Booking System API")
                        .description("Production-grade RESTful API for movie show seat booking with strict concurrency protection, zero double-booking, pre-show cancellation, and automated seat management.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Softnix Engineering Assessment")
                                .email("dev@softnix.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}

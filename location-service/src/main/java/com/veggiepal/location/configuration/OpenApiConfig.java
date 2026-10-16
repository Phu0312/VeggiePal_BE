package com.veggiepal.location.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

// No bearerAuth scheme: every endpoint here is public.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI veggiePalOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("VeggiePal Location Service API")
                                .version("1.0")
                                .description("Nearby vegetarian and vegan restaurants for VeggiePal")
                )
                .servers(
                        List.of(
                                new Server()
                                        .url("/api")
                                        .description("API Gateway"),
                                new Server()
                                        .url("/")
                                        .description("Direct Service")
                        )
                );
    }
}

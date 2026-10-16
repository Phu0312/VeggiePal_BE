package com.veggiepal.recipe.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI recipeOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("VeggiePal Recipe Service API")
                                .version("1.0")
                                .description("Recipe and ingredient APIs for VeggiePal")
                )
                .servers(
                        List.of(
                                new Server().url("/api").description("API Gateway"),
                                new Server().url("/").description("Direct Service")
                        )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                )
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
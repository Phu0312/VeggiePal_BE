package com.veggiepal.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI veggiePalOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title(
                                        "VeggiePal Identity Service API"
                                )
                                .version("1.0")
                                .description(
                                        "Identity APIs for VeggiePal"
                                )
                );
    }
}
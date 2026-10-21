package com.veggiepal.payment.configuration;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    OpenAPI paymentOpenApi() {
        String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("VeggiePal Payment Service API").version("1.0")
                        .description("Payment checkout and subscription APIs for VeggiePal"))
                .servers(List.of(new Server().url("/api").description("API Gateway"),
                        new Server().url("/").description("Direct Service")))
                .components(new Components().addSecuritySchemes(bearer,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearer));
    }
}

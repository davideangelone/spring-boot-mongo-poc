package it.davideangelone.mongopoc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI mongoCrudOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MongoDB Users & Items API")
                .version("1.0.0")
                .description("POC REST CRUD con Spring Boot 4, Spring Data MongoDB e item embedded per utente."));
    }
}

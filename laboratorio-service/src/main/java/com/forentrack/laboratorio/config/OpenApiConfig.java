package com.forentrack.laboratorio.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/** Documentación OpenAPI. El servidor relativo "/" permite probarla también desde el gateway. */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("Servidor actual o API Gateway")))
                .info(new Info()
                        .title("laboratorio-service")
                        .version("2.0.0")
                        .description("ForenTrack: Laboratorio forense: solicitudes de análisis, peritajes, análisis digital y dictámenes. Roles con acciones: INVESTIGADOR (solicita), JEFE_LABORATORIO (asigna/revisa), PERITO y ANALISTA_DIGITAL (analizan)."));
    }
}

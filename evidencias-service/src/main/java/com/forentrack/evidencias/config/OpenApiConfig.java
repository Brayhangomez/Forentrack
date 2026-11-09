package com.forentrack.evidencias.config;

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
                        .title("evidencias-service")
                        .version("2.0.0")
                        .description("ForenTrack: Evidencias: registro, acta de recolección, estados y descarte. Roles con acciones: TECNICO_CAMPO y REVISOR_LEGAL; transiciones internas para CUSTODIO, PERITO, ANALISTA_DIGITAL y JEFE_LABORATORIO."));
    }
}

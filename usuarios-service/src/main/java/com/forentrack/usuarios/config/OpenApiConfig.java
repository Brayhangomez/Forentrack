package com.forentrack.usuarios.config;

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
                        .title("usuarios-service")
                        .version("2.0.0")
                        .description("ForenTrack: Autenticación JWT, usuarios y roles. Roles: ADMINISTRADOR gestiona cuentas; FISCAL y JEFE_LABORATORIO consultan su directorio."));
    }
}

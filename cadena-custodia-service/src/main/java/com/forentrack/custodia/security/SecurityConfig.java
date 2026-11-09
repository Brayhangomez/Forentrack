package com.forentrack.custodia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Primer nivel de seguridad: reglas por método y ruta. El segundo nivel es @PreAuthorize en el controlador. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Roles que consultan la cadena de custodia (todos menos ADMINISTRADOR). */
    private static final String[] LECTURA = {"INVESTIGADOR", "PERITO", "CUSTODIO", "FISCAL", "AUDITOR",
            "TECNICO_CAMPO", "ANALISTA_DIGITAL", "JEFE_LABORATORIO", "REVISOR_LEGAL"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           ManejadoresSeguridad manejadores) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/custodia/entregas").hasRole("TECNICO_CAMPO")
                .requestMatchers(HttpMethod.POST, "/api/v1/custodia/recepciones",
                                 "/api/v1/custodia/traslados",
                                 "/api/v1/custodia/devoluciones").hasRole("CUSTODIO")
                .requestMatchers(HttpMethod.POST, "/api/v1/custodia/evidencias/*/validaciones").hasRole("REVISOR_LEGAL")
                .requestMatchers(HttpMethod.GET, "/api/v1/custodia/**").hasAnyRole(LECTURA)
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(manejadores).accessDeniedHandler(manejadores))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

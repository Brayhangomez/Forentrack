package com.forentrack.evidencias.security;

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           ManejadoresSeguridad manejadores) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/evidencias").hasRole("TECNICO_CAMPO")
                .requestMatchers(HttpMethod.PUT, "/api/v1/evidencias/*/recoleccion").hasRole("TECNICO_CAMPO")
                .requestMatchers(HttpMethod.POST, "/api/v1/evidencias/*/descarte").hasRole("REVISOR_LEGAL")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/evidencias/*/estado")
                    .hasAnyRole("CUSTODIO", "PERITO", "ANALISTA_DIGITAL", "JEFE_LABORATORIO")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(manejadores).accessDeniedHandler(manejadores))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

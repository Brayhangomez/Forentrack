package com.forentrack.laboratorio.security;

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

    private static final String BASE = "/api/v1/laboratorio/solicitudes";

    /** Roles que consultan el laboratorio. */
    private static final String[] LECTURA = {"INVESTIGADOR", "FISCAL", "PERITO", "ANALISTA_DIGITAL",
            "JEFE_LABORATORIO", "REVISOR_LEGAL", "AUDITOR"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           ManejadoresSeguridad manejadores) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, BASE).hasRole("INVESTIGADOR")
                .requestMatchers(HttpMethod.POST, BASE + "/*/asignacion", BASE + "/*/aprobacion",
                                 BASE + "/*/rechazo").hasRole("JEFE_LABORATORIO")
                .requestMatchers(HttpMethod.POST, BASE + "/*/peritaje/inicio", BASE + "/*/peritaje/dictamen")
                    .hasRole("PERITO")
                .requestMatchers(HttpMethod.POST, BASE + "/*/digital/inicio", BASE + "/*/digital/hash",
                                 BASE + "/*/digital/informe").hasRole("ANALISTA_DIGITAL")
                .requestMatchers(HttpMethod.GET, "/api/v1/laboratorio/**").hasAnyRole(LECTURA)
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(manejadores).accessDeniedHandler(manejadores))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

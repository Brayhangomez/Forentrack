package com.forentrack.casos.security;

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
                .requestMatchers(HttpMethod.POST, "/api/v1/casos",
                                 "/api/v1/casos/*/inicio-investigacion",
                                 "/api/v1/casos/*/notas").hasRole("INVESTIGADOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/casos/*/asignacion",
                                 "/api/v1/casos/*/suspension",
                                 "/api/v1/casos/*/reactivacion",
                                 "/api/v1/casos/*/cierre").hasRole("FISCAL")
                .requestMatchers(HttpMethod.POST, "/api/v1/casos/*/concepto-legal").hasRole("REVISOR_LEGAL")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(manejadores).accessDeniedHandler(manejadores))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

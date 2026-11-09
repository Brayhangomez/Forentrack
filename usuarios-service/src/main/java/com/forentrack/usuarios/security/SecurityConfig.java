package com.forentrack.usuarios.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
                .requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**",
                                 "/swagger-ui.html", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/directorio/investigadores").hasRole("FISCAL")
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/directorio/laboratorio").hasRole("JEFE_LABORATORIO")
                .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(manejadores).accessDeniedHandler(manejadores))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

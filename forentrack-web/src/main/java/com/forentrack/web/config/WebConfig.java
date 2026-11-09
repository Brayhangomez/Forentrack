package com.forentrack.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SesionInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/error", "/favicon.ico", "/favicon.svg",
                                     "/css/**", "/js/**", "/img/**");
    }
}

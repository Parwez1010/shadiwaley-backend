package com.shadiwaley.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
        "http://localhost:4200",
        "http://localhost:3000",
        "http://localhost:8080",

        // Vercel applications
        "https://shadiwaley-admin-app.vercel.app",
        "https://shadiwaley-customer-dun.vercel.app",
        "https://shadiwaley-admin-ge0t1e9dh-parwez1010s-projects.vercel.app",

        // Current production domains
        "https://qabiltu.in",
        "https://www.qabiltu.in",

        // Legacy domains
        "https://shadiwaley.com",
        "https://admin.shadiwaley.com"

        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PATCH",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        config.setExposedHeaders(List.of(
                "Authorization"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}

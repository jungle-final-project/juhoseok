package com.buildgraph.prototype.common;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {
    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    );

    private List<String> allowedOrigins = new ArrayList<>(DEFAULT_ALLOWED_ORIGINS);

    public void setAllowedOrigins(List<String> allowedOrigins) {
        if (allowedOrigins == null) {
            this.allowedOrigins = new ArrayList<>(DEFAULT_ALLOWED_ORIGINS);
            return;
        }
        this.allowedOrigins = new ArrayList<>(allowedOrigins);
    }

    @Bean
    WebMvcConfigurer corsConfigurer() {
        String[] origins = normalizedAllowedOrigins().toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    private List<String> normalizedAllowedOrigins() {
        List<String> normalized = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
        return normalized.isEmpty() ? DEFAULT_ALLOWED_ORIGINS : normalized;
    }
}

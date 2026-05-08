package org.exemplo.belloryadmin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS do Bellory-Admin-API.
 * - admin.bellory.com.br: front admin em prod
 * - bellory.com.br: site institucional (necessario para /api/v1/tracking publico)
 * - localhost:*: dev local (front admin e teste)
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(
                                "https://admin.bellory.com.br",
                                "https://bellory.com.br",
                                "https://*.bellory.com.br",
                                "https://*.vercel.app",
                                "http://localhost:*",
                                "https://localhost:*"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Content-Type", "Content-Length")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}

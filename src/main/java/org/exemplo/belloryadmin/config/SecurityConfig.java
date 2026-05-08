package org.exemplo.belloryadmin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig do Bellory-Admin-API.
 *
 * <p>Rotas:</p>
 * <ul>
 *   <li>{@code /api/v1/admin/auth/**} - publico (login admin)</li>
 *   <li>{@code /api/v1/tracking} - publico (ingestao do site bellory.com.br)</li>
 *   <li>{@code /api/v1/webhook/payment} - publico (Payment API valida via token proprio)</li>
 *   <li>{@code /api/v1/admin/**} - exige role PLATFORM_ADMIN</li>
 *   <li>{@code /swagger-ui/**}, {@code /v3/api-docs/**}, {@code /actuator/**} - publicos</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                // Login admin (publico)
                                "/api/v1/admin/auth/**",

                                // Tracking publico (site bellory.com.br)
                                "/api/v1/tracking/**",

                                // Webhook Payment API (validado por token proprio do payload)
                                "/api/v1/webhook/payment",

                                // Documentacao e healthcheck
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/actuator/**",
                                "/health",

                                // Uploads publicos
                                "/uploads/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**")
                            .hasAnyRole("PLATFORM_ADMIN", "SUPERADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

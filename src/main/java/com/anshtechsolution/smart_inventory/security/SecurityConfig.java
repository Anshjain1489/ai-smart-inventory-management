package com.anshtechsolution.smart_inventory.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.SecretKey;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.issuer}")
    private String jwtIssuer;

    // Decode and validate the JWT secret.
    @Bean
    public SecretKey jwtSecretKey() {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT secret must be a valid Base64 string",
                    exception
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must contain at least 32 decoded bytes"
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // JWT token generation.
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    // JWT token validation.
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(jwtIssuer)
        );

        return decoder;
    }

    // Convert JWT roles into Spring Security authorities.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            List<String> roles = jwt.getClaimAsStringList("roles");

            if (roles == null) {
                return List.<org.springframework.security.core.GrantedAuthority>of();
            }

            return roles.stream()
                    .map(role -> (org.springframework.security.core.GrantedAuthority)
                            new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });

        return converter;
    }

    // Password hashing.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Authentication manager.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Main security rules.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public authentication endpoints.
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup"
                        ).permitAll()

                        .requestMatchers("/actuator/health").permitAll()

                        // Protect all other Actuator endpoints.
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")

                        // Public health and API documentation.
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // AI endpoints: ADMIN and MANAGER only.
                        .requestMatchers("/api/v1/ai/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // Read-only GET endpoints: all three roles.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/products/**",
                                "/api/v1/inventory/**",
                                "/api/v1/sales/**",
                                "/api/v1/purchase-orders/**",
                                "/api/v1/suppliers/**",
                                "/api/v1/reports/**",
                                "/api/v1/inventory-risk/**",
                                "/api/v1/dashboard/**",
                                "/api/v1/analytics/**"
                        ).hasAnyRole("ADMIN", "MANAGER", "VIEWER")

                        // Create operations: ADMIN and MANAGER.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/products/**",
                                "/api/v1/inventory/**",
                                "/api/v1/sales/**",
                                "/api/v1/purchase-orders/**",
                                "/api/v1/suppliers/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // Update operations: ADMIN and MANAGER.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/products/**",
                                "/api/v1/purchase-orders/**",
                                "/api/v1/suppliers/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/products/**",
                                "/api/v1/purchase-orders/**",
                                "/api/v1/suppliers/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // Delete operations: ADMIN and MANAGER.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/products/**",
                                "/api/v1/purchase-orders/**",
                                "/api/v1/suppliers/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // Deny anything not explicitly configured.
                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}
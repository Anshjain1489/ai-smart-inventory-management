package com.anshtechsolution.smart_inventory.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    private SecurityConfig securityConfig;
    private String validSecret;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();

        byte[] keyBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(keyBytes);

        validSecret = Base64.getEncoder().encodeToString(keyBytes);

        ReflectionTestUtils.setField(
                securityConfig, "jwtSecret", validSecret
        );

        ReflectionTestUtils.setField(
                securityConfig, "jwtIssuer", "smart-inventory-test"
        );
    }

    @Test
    void jwtSecretKeyShouldCreateValidSecretKey() {
        SecretKey secretKey = securityConfig.jwtSecretKey();

        assertThat(secretKey).isNotNull();
        assertThat(secretKey.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(secretKey.getEncoded()).hasSize(32);
    }

    @Test
    void jwtSecretKeyShouldRejectInvalidBase64() {
        ReflectionTestUtils.setField(
                securityConfig, "jwtSecret", "invalid-base64!!!"
        );

        assertThatThrownBy(() -> securityConfig.jwtSecretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid Base64");
    }

    @Test
    void jwtSecretKeyShouldRejectShortSecret() {
        String shortSecret = Base64.getEncoder()
                .encodeToString(new byte[16]);

        ReflectionTestUtils.setField(
                securityConfig, "jwtSecret", shortSecret
        );

        assertThatThrownBy(() -> securityConfig.jwtSecretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 decoded bytes");
    }

    @Test
    void jwtEncoderShouldCreateEncoder() {
        SecretKey secretKey = securityConfig.jwtSecretKey();

        JwtEncoder encoder = securityConfig.jwtEncoder(secretKey);

        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(
                org.springframework.security.oauth2.jwt.NimbusJwtEncoder.class
        );
    }

    @Test
    void jwtDecoderShouldCreateDecoder() {
        SecretKey secretKey = securityConfig.jwtSecretKey();

        JwtDecoder decoder = securityConfig.jwtDecoder(secretKey);

        assertThat(decoder).isNotNull();
        assertThat(decoder).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void jwtAuthenticationConverterShouldExtractAdminRole() {
        var converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = createJwt(List.of("ADMIN"));

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .filteredOn(authority -> authority.toString().startsWith("ROLE_"))
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void jwtAuthenticationConverterShouldExtractMultipleRoles() {
        var converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = createJwt(List.of("ADMIN", "MANAGER", "VIEWER"));

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .filteredOn(authority -> authority.toString().startsWith("ROLE_"))
                .containsExactlyInAnyOrder(
                        "ROLE_ADMIN",
                        "ROLE_MANAGER",
                        "ROLE_VIEWER"
                );
    }

    @Test
    void jwtAuthenticationConverterShouldReturnEmptyAuthoritiesWhenRolesMissing() {
        var converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("ansh")
                .claim("iss", "smart-inventory-test")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .filteredOn(authority -> authority.toString().startsWith("ROLE_"))
                .isEmpty();
    }

    @Test
    void passwordEncoderShouldEncodeAndVerifyPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String rawPassword = "StrongPassword123!";
        String encodedPassword = encoder.encode(rawPassword);

        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(encoder.matches("WrongPassword", encodedPassword)).isFalse();
    }

    private Jwt createJwt(List<String> roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("ansh")
                .claim("iss", "smart-inventory-test")
                .claim("roles", roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();
    }
}
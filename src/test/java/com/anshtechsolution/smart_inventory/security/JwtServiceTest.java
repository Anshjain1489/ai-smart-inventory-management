package com.anshtechsolution.smart_inventory.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    private static final String ISSUER = "smart-inventory-test";
    private static final long TOKEN_MINUTES = 30;

    @BeforeEach
    void setUp() {

        // Generate a test-only 256-bit key.
        byte[] keyBytes = new byte[32];

        new java.security.SecureRandom().nextBytes(keyBytes);

        SecretKey secretKey = new SecretKeySpec(
                keyBytes,
                "HmacSHA256"
        );

        // Real HS256 encoder.
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );

        // Real HS256 decoder.
        jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        jwtService = new JwtService(
                jwtEncoder,
                TOKEN_MINUTES,
                ISSUER
        );
    }

    // 1. Generate JWT successfully
    @Test
    void generateTokenShouldReturnValidJwt() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        String token = jwtService.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isBlank());

        // JWT contains three dot-separated sections.
        assertEquals(3, token.split("\\.").length);
    }

    // 2. Verify token subject
    @Test
    void generateTokenShouldContainUsernameAsSubject() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertEquals("ansh", decodedJwt.getSubject());
    }

    // 3. Verify issuer
    @Test
    void generateTokenShouldContainCorrectIssuer() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertThat(decodedJwt.getClaimAsString("iss"))
                .isEqualTo("smart-inventory-test");
    }

    // 4. Verify ADMIN role
    @Test
    void generateTokenShouldContainAdminRole() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertEquals(
                List.of("ADMIN"),
                decodedJwt.getClaimAsStringList("roles")
        );
    }

    // 5. Verify multiple roles
    @Test
    void generateTokenShouldContainMultipleValidRoles() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_MANAGER"),
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        )
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertEquals(
                3,
                decodedJwt.getClaimAsStringList("roles").size()
        );

        assertTrue(decodedJwt.getClaimAsStringList("roles")
                .containsAll(List.of("ADMIN", "MANAGER", "VIEWER")));
    }

    // 6. Exclude invalid roles
    @Test
    void generateTokenShouldExcludeInvalidRoles() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_FACTOR_PASSWORD"),
                                new SimpleGrantedAuthority("ROLE_UNKNOWN")
                        )
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertEquals(
                List.of("ADMIN"),
                decodedJwt.getClaimAsStringList("roles")
        );
    }

    // 7. Remove duplicate roles
    @Test
    void generateTokenShouldRemoveDuplicateRoles() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertEquals(
                List.of("ADMIN"),
                decodedJwt.getClaimAsStringList("roles")
        );
    }

    // 8. Verify token issue and expiry timestamps
    @Test
    void generateTokenShouldContainCorrectExpiration() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertNotNull(decodedJwt.getIssuedAt());
        assertNotNull(decodedJwt.getExpiresAt());

        long actualMinutes = java.time.Duration.between(
                decodedJwt.getIssuedAt(),
                decodedJwt.getExpiresAt()
        ).toMinutes();

        assertEquals(TOKEN_MINUTES, actualMinutes);
    }

    // 9. Verify expiration duration in seconds
    @Test
    void getAccessTokenExpirationSecondsShouldReturn1800() {

        assertEquals(
                1800L,
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    // 10. Verify empty roles
    @Test
    void generateTokenShouldSupportUserWithoutAuthorities() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "ansh",
                        null,
                        List.of()
                );

        String token = jwtService.generateToken(authentication);

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertTrue(
                decodedJwt.getClaimAsStringList("roles").isEmpty()
        );
    }
}
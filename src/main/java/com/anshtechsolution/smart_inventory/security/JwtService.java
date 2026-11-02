package com.anshtechsolution.smart_inventory.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long accessTokenMinutes;
    private final String jwtIssuer;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.access-token-minutes}")
            long accessTokenMinutes,
            @Value("${app.security.jwt.issuer}")
            String jwtIssuer
    ) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenMinutes = accessTokenMinutes;
        this.jwtIssuer = jwtIssuer;
    }

    public String generateToken(Authentication authentication) {

        Instant issuedAt = Instant.now();

        Instant expiresAt = issuedAt.plus(
                accessTokenMinutes,
                ChronoUnit.MINUTES
        );

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority ->
                        authority.equals("ROLE_ADMIN")
                                || authority.equals("ROLE_MANAGER")
                                || authority.equals("ROLE_VIEWER")
                )
                .map(authority -> authority.substring(5))
                .distinct()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        JwsHeader headers = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(headers, claims)
        ).getTokenValue();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenMinutes * 60;
    }
}
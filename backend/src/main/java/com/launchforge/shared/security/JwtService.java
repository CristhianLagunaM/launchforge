package com.launchforge.shared.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public record IssuedToken(
            String tokenValue,
            Instant issuedAt,
            Instant expiresAt
    ) {
    }

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtProperties = jwtProperties;
    }

    public IssuedToken issueToken(AuthUserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.expirationSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(principal.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", principal.email())
                .claim("roles", principal.roles())
                .build();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
        return new IssuedToken(tokenValue, issuedAt, expiresAt);
    }

    public String generateToken(AuthUserPrincipal principal) {
        return issueToken(principal).tokenValue();
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(decode(token).getSubject());
    }

    public String extractEmail(String token) {
        return decode(token).getClaimAsString("email");
    }

    public List<String> extractRoles(String token) {
        return decode(token).getClaimAsStringList("roles");
    }
}

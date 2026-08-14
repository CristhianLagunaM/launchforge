package com.launchforge.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("unit-test-secret-value-with-32-chars", 3600);
        SecretKey secretKey = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
        JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        jwtService = new JwtService(jwtEncoder, jwtDecoder, properties);
    }

    @Test
    void generatesAndValidatesTokenWithRequiredClaims() {
        AuthUserPrincipal principal = new AuthUserPrincipal(
                UUID.randomUUID(),
                "admin@launchforge.dev",
                "hash",
                true,
                List.of("ADMIN")
        );

        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(principal.id());
        assertThat(jwtService.extractEmail(token)).isEqualTo(principal.email());
        assertThat(jwtService.extractRoles(token)).containsExactly("ADMIN");
        assertThat(jwtService.decode(token).getIssuedAt()).isNotNull();
        assertThat(jwtService.decode(token).getExpiresAt()).isAfter(jwtService.decode(token).getIssuedAt());
    }
}

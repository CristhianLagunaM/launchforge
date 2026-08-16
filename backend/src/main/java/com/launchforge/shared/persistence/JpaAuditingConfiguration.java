package com.launchforge.shared.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "authenticatedAuditorAware")
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class JpaAuditingConfiguration {

    @Bean
    public AuditorAware<UUID> authenticatedAuditorAware() {
        return () -> Optional
                .ofNullable(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
                .filter(authentication ->
                        authentication != null
                                && authentication.isAuthenticated()
                )
                .map(authentication ->
                        Objects.requireNonNull(
                                authentication.getPrincipal(),
                                "Authentication principal must not be null"
                        )
                )
                .filter(principal ->
                        principal instanceof Jwt
                )
                .map(principal ->
                        (Jwt) principal
                )
                .map(jwt ->
                        Objects.requireNonNull(
                                jwt.getSubject(),
                                "JWT subject must not be null"
                        )
                )
                .map(subject ->
                        UUID.fromString(subject)
                );
    }
}

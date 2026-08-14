package com.launchforge.auth.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        String accessToken,
        String tokenType,
        Instant issuedAt,
        Instant expiresAt,
        UserView user
) {

    public record UserView(
            UUID id,
            String email,
            String firstName,
            String lastName,
            List<String> roles
    ) {
    }
}

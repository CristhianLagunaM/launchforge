package com.launchforge.auth.api.dto;

import com.launchforge.auth.application.dto.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant issuedAt,
        Instant expiresAt,
        UserResponse user
) {

    public static AuthResponse from(AuthenticatedUser authenticatedUser) {
        return new AuthResponse(
                authenticatedUser.accessToken(),
                authenticatedUser.tokenType(),
                authenticatedUser.issuedAt(),
                authenticatedUser.expiresAt(),
                UserResponse.from(authenticatedUser.user())
        );
    }

    public record UserResponse(
            UUID id,
            String email,
            String firstName,
            String lastName,
            List<String> roles
    ) {
        static UserResponse from(AuthenticatedUser.UserView userView) {
            return new UserResponse(
                    userView.id(),
                    userView.email(),
                    userView.firstName(),
                    userView.lastName(),
                    userView.roles()
            );
        }
    }
}

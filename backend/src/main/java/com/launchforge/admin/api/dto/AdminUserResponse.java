package com.launchforge.admin.api.dto;

import com.launchforge.persistence.model.identity.User;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(UUID id, String email, String firstName, String lastName, boolean enabled, List<String> roles) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                Boolean.TRUE.equals(user.getEnabled()), user.getUserRoles().stream().map(r -> r.getRole().getName()).sorted().toList());
    }
}

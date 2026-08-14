package com.launchforge.shared.security;

import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.identity.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthUserPrincipal(
        UUID id,
        String email,
        String passwordHash,
        boolean enabled,
        List<String> roles
) implements UserDetails {

    public static AuthUserPrincipal fromUser(User user) {
        List<String> roleNames = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> role.getName().toUpperCase())
                .toList();
        return new AuthUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getEnabled()),
                roleNames
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

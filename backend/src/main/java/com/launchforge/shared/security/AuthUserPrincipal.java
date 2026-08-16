package com.launchforge.shared.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.launchforge.persistence.model.identity.Role;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.identity.UserRole;

public record AuthUserPrincipal(
        UUID id,
        String email,
        String passwordHash,
        boolean enabled,
        List<String> roles
) implements UserDetails {

    public static AuthUserPrincipal fromUser(User user) {
        List<String> roleNames = new ArrayList<>();

        for (UserRole userRole : user.getUserRoles()) {
            if (userRole == null) {
                continue;
            }

            Role role = userRole.getRole();

            if (role == null || role.getName() == null) {
                continue;
            }

            roleNames.add(
                    role.getName().toUpperCase()
            );
        }

        return new AuthUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getEnabled()),
                List.copyOf(roleNames)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String role : roles) {
            if (role != null) {
                authorities.add(
                        new SimpleGrantedAuthority(
                                "ROLE_" + role
                        )
                );
            }
        }

        return List.copyOf(authorities);
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

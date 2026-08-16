package com.launchforge.admin.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.launchforge.admin.api.dto.AdminUserResponse;
import com.launchforge.admin.api.dto.AdminUserUpdateRequest;
import com.launchforge.auth.infrastructure.RoleRepository;
import com.launchforge.auth.infrastructure.UserRepository;
import com.launchforge.persistence.model.identity.UserRole;
import com.launchforge.persistence.model.identity.UserRoleId;
import com.launchforge.shared.exception.ApiNotFoundException;

@Service
public class AdminUserService {

    private final UserRepository users;
    private final RoleRepository roles;

    public AdminUserService(
            UserRepository users,
            RoleRepository roles
    ) {
        this.users = users;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return users.findAll()
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse update(
            UUID id,
            AdminUserUpdateRequest request
    ) {
        UUID userId = Objects.requireNonNull(
                id,
                "User id must not be null"
        );

        var user = users.findById(userId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Usuario no encontrado",
                        "No existe el usuario solicitado.",
                        "users/not-found"
                ));

        var role = roles.findByNameIgnoreCase(request.role())
                .orElseThrow(() -> new ApiNotFoundException(
                        "Rol no encontrado",
                        "El rol indicado no existe.",
                        "users/role-not-found"
                ));

        user.setEnabled(request.enabled());
        user.getUserRoles().clear();

        var assignment = new UserRole();
        assignment.setUser(user);
        assignment.setRole(role);

        var key = new UserRoleId();
        key.setUserId(user.getId());
        key.setRoleId(role.getId());
        assignment.setId(key);

        user.getUserRoles().add(assignment);

        return AdminUserResponse.from(users.save(user));
    }
}

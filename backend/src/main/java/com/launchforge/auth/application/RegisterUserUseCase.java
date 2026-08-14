package com.launchforge.auth.application;

import com.launchforge.auth.api.dto.RegisterRequest;
import com.launchforge.auth.application.dto.AuthenticatedUser;
import com.launchforge.auth.infrastructure.RoleRepository;
import com.launchforge.auth.infrastructure.UserRepository;
import com.launchforge.persistence.model.identity.Role;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.identity.UserRole;
import com.launchforge.shared.exception.DuplicateEmailException;
import com.launchforge.shared.security.ApplicationRole;
import com.launchforge.shared.security.AuthUserPrincipal;
import com.launchforge.shared.security.JwtService;
import java.time.Instant;
import java.util.Comparator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterUserUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthenticatedUser register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEnabled(Boolean.TRUE);

        Role customerRole = roleRepository.findByNameIgnoreCase(ApplicationRole.CUSTOMER.name())
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER not found"));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(customerRole);
        user.getUserRoles().add(userRole);

        User savedUser = userRepository.save(user);
        AuthUserPrincipal principal = AuthUserPrincipal.fromUser(savedUser);
        String token = jwtService.generateToken(principal);
        Instant issuedAt = jwtService.decode(token).getIssuedAt();
        Instant expiresAt = jwtService.decode(token).getExpiresAt();

        return new AuthenticatedUser(
                token,
                "Bearer",
                issuedAt,
                expiresAt,
                new AuthenticatedUser.UserView(
                        savedUser.getId(),
                        savedUser.getEmail(),
                        savedUser.getFirstName(),
                        savedUser.getLastName(),
                        principal.roles().stream().sorted(Comparator.naturalOrder()).toList()
                )
        );
    }
}

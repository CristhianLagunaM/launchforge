package com.launchforge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.launchforge.auth.api.dto.LoginRequest;
import com.launchforge.auth.application.LoginUseCase;
import com.launchforge.auth.application.dto.AuthenticatedUser;
import com.launchforge.auth.infrastructure.UserRepository;
import com.launchforge.persistence.model.identity.Role;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.identity.UserRole;
import com.launchforge.shared.exception.InvalidCredentialsException;
import com.launchforge.shared.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(authenticationManager, userRepository, jwtService);
    }

    @Test
    void returnsAuthenticatedUserWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("admin@launchforge.dev", "LaunchForge123!");
        User user = buildUser();
        Instant issuedAt = Instant.parse("2026-08-14T15:00:00Z");
        Instant expiresAt = issuedAt.plusSeconds(3600);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated("admin@launchforge.dev", null, java.util.List.of());

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(user));
        when(jwtService.issueToken(any()))
                .thenReturn(new JwtService.IssuedToken("jwt-token", issuedAt, expiresAt));

        AuthenticatedUser response = loginUseCase.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("admin@launchforge.dev");
        assertThat(response.user().roles()).containsExactly("ADMIN");
        verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @Test
    void throwsInvalidCredentialsWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("admin@launchforge.dev", "wrong-password");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> loginUseCase.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setEmail("admin@launchforge.dev");
        user.setPasswordHash("hash");
        user.setFirstName("Admin");
        user.setLastName("LaunchForge");
        user.setEnabled(true);

        Role role = new Role();
        role.setId((short) 1);
        role.setName("ADMIN");
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);
        return user;
    }
}

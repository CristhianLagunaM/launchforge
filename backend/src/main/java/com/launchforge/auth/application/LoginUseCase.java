package com.launchforge.auth.application;

import com.launchforge.auth.api.dto.LoginRequest;
import com.launchforge.auth.application.dto.AuthenticatedUser;
import com.launchforge.auth.infrastructure.UserRepository;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.shared.exception.InvalidCredentialsException;
import com.launchforge.shared.security.AuthUserPrincipal;
import com.launchforge.shared.security.JwtService;
import java.util.Comparator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LoginUseCase(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
            );
        } catch (BadCredentialsException | DisabledException exception) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        AuthUserPrincipal principal = AuthUserPrincipal.fromUser(user);
        JwtService.IssuedToken issuedToken = jwtService.issueToken(principal);

        return new AuthenticatedUser(
                issuedToken.tokenValue(),
                "Bearer",
                issuedToken.issuedAt(),
                issuedToken.expiresAt(),
                new AuthenticatedUser.UserView(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        principal.roles().stream().sorted(Comparator.naturalOrder()).toList()
                )
        );
    }
}

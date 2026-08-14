package com.launchforge.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordHashingTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void hashesPasswordWithBcryptAndVerifiesIt() {
        String rawPassword = "LaunchForge123!";

        String hash = passwordEncoder.encode(rawPassword);

        assertThat(hash).isNotEqualTo(rawPassword);
        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, hash)).isTrue();
    }
}

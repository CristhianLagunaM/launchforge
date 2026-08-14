package com.launchforge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class LaunchForgeApplicationTest {

    @Test
    void applicationIsBootstrappedAsSpringBootApplication() {
        assertTrue(LaunchForgeApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}


package com.launchforge.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerCreatesCustomerAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"new.customer@launchforge.dev",
                                  "password":"LaunchForge123!",
                                  "firstName":"New",
                                  "lastName":"Customer"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("new.customer@launchforge.dev"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.roles", hasItem("CUSTOMER")));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"admin@launchforge.dev",
                                  "password":"LaunchForge123!",
                                  "firstName":"Admin",
                                  "lastName":"Duplicate"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void loginReturnsJwtWhenCredentialsAreValid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"admin@launchforge.dev",
                                  "password":"launchforge-demo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@launchforge.dev"))
                .andExpect(jsonPath("$.user.roles", hasItem("ADMIN")));
    }

    @Test
    void loginRejectsIncorrectCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"admin@launchforge.dev",
                                  "password":"wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointRejectsRequestsWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void protectedEndpointRejectsCustomerRoleForAdminOnlyRoute() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ping")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111112")
                                        .claim("email", "customer@launchforge.dev")
                                        .claim("roles", java.util.List.of("CUSTOMER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }
}

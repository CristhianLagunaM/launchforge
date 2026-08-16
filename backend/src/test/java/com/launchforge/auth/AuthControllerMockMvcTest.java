package com.launchforge.auth;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    private static final UUID ADMIN_ID =
            UUID.fromString(
                    "91111111-2222-3333-4444-555555555555"
            );

    private static final String ADMIN_EMAIL =
            "auth.admin@launchforge.dev";

    private static final String ADMIN_PASSWORD =
            "LaunchForge123!";

    private static final String REGISTER_EMAIL =
            "new.customer@launchforge.dev";

    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFixtures() {
        cleanupRegisteredCustomer();
        ensureAdminExists();
    }

    @Test
    void registerCreatesCustomerAndReturnsToken() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email":"new.customer@launchforge.dev",
                                          "password":"LaunchForge123!",
                                          "firstName":"New",
                                          "lastName":"Customer"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.accessToken"
                        ).isNotEmpty()
                )
                .andExpect(
                        jsonPath(
                                "$.tokenType"
                        ).value(
                                "Bearer"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.user.email"
                        ).value(
                                REGISTER_EMAIL
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.user.passwordHash"
                        ).doesNotExist()
                )
                .andExpect(
                        jsonPath(
                                "$.user.roles",
                                hasItem("CUSTOMER")
                        )
                );
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email":"auth.admin@launchforge.dev",
                                          "password":"LaunchForge123!",
                                          "firstName":"Admin",
                                          "lastName":"Duplicate"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        header().string(
                                "Content-Type",
                                containsString(
                                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(
                                409
                        )
                );
    }

    @Test
    void loginReturnsJwtWhenCredentialsAreValid() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email":"auth.admin@launchforge.dev",
                                          "password":"LaunchForge123!"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.accessToken"
                        ).isNotEmpty()
                )
                .andExpect(
                        jsonPath(
                                "$.user.email"
                        ).value(
                                ADMIN_EMAIL
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.user.roles",
                                hasItem("ADMIN")
                        )
                );
    }

    @Test
    void loginRejectsIncorrectCredentials() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email":"auth.admin@launchforge.dev",
                                          "password":"wrong-password"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header().string(
                                "Content-Type",
                                containsString(
                                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(
                                401
                        )
                );
    }

    @Test
    void protectedEndpointRejectsRequestsWithoutJwt() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/ping")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header().string(
                                "Content-Type",
                                containsString(
                                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                                )
                        )
                );
    }

    @Test
    void protectedEndpointRejectsCustomerRoleForAdminOnlyRoute()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/admin/ping")
                                .with(
                                        jwt()
                                                .jwt(
                                                        jwt ->
                                                                jwt
                                                                        .subject(
                                                                                "11111111-1111-1111-1111-111111111112"
                                                                        )
                                                                        .claim(
                                                                                "email",
                                                                                "customer@launchforge.dev"
                                                                        )
                                                                        .claim(
                                                                                "roles",
                                                                                List.of(
                                                                                        "CUSTOMER"
                                                                                )
                                                                        )
                                                )
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"
                                                        )
                                                )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        header().string(
                                "Content-Type",
                                containsString(
                                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                                )
                        )
                );
    }

    private void ensureAdminExists() {
        Long adminRoleId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM roles
                        WHERE name = 'ADMIN'
                        """,
                        Long.class
                );

        if (adminRoleId == null) {
            throw new IllegalStateException(
                    "ADMIN role is not available."
            );
        }

        jdbcTemplate.update(
                """
                INSERT INTO users (
                    id,
                    email,
                    password_hash,
                    first_name,
                    last_name,
                    enabled,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, TRUE, NOW(), NOW())
                ON CONFLICT (id)
                DO UPDATE SET
                    email = EXCLUDED.email,
                    password_hash = EXCLUDED.password_hash,
                    first_name = EXCLUDED.first_name,
                    last_name = EXCLUDED.last_name,
                    enabled = TRUE,
                    updated_at = NOW()
                """,
                ADMIN_ID,
                ADMIN_EMAIL,
                passwordEncoder.encode(
                        ADMIN_PASSWORD
                ),
                "Auth",
                "Admin"
        );

        jdbcTemplate.update(
                """
                DELETE FROM user_roles
                WHERE user_id = ?
                """,
                ADMIN_ID
        );

        jdbcTemplate.update(
                """
                INSERT INTO user_roles (
                    user_id,
                    role_id
                )
                VALUES (?, ?)
                """,
                ADMIN_ID,
                adminRoleId
        );
    }

    private void cleanupRegisteredCustomer() {
        List<UUID> ids =
                jdbcTemplate.query(
                        """
                        SELECT id
                        FROM users
                        WHERE email = ?
                        """,
                        (resultSet, rowNum) ->
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                        REGISTER_EMAIL
                );

        for (UUID id : ids) {
            jdbcTemplate.update(
                    """
                    DELETE FROM user_roles
                    WHERE user_id = ?
                    """,
                    id
            );

            jdbcTemplate.update(
                    """
                    DELETE FROM users
                    WHERE id = ?
                    """,
                    id
            );
        }
    }
}

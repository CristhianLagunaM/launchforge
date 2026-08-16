package com.launchforge.inventory;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventoryControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    private static final UUID ADMIN_ID =
            UUID.fromString(
                    "95555555-5555-5555-5555-555555555555"
            );

    private static final String ADMIN_EMAIL =
            "inventory.admin@launchforge.dev";

    private static final String PASSWORD_HASH =
            "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFixtures() {
        ensureAdminExists();

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET
                    available_quantity = 8,
                    reserved_quantity = 0,
                    version = 0
                WHERE product_id = ?
                """,
                productId()
        );
    }

    @Test
    void getInventoryRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(
                        get("/api/v1/inventory")
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
    void patchInventoryRejectsCustomerRole() throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/inventory/{productId}",
                                productId()
                        )
                                .with(
                                        customerJwt()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validAdjustmentPayload()
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

    @Test
    void patchInventoryAllowsAdminRole() throws Exception {
        UUID productId =
                productId();

        mockMvc.perform(
                        patch(
                                "/api/v1/inventory/{productId}",
                                productId
                        )
                                .with(
                                        adminJwt()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validAdjustmentPayload()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.productId",
                                is(productId.toString())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.availableQuantity",
                                is(10)
                        )
                );
    }

    @Test
    void patchInventoryReturnsConflictForStaleVersion() throws Exception {
        UUID productId =
                productId();

        Long currentVersion =
                currentVersion(productId);

        long staleVersion =
                currentVersion + 999;

        mockMvc.perform(
                        patch(
                                "/api/v1/inventory/{productId}",
                                productId
                        )
                                .with(
                                        adminJwt()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "operation":"INCREASE",
                                          "quantity":1,
                                          "version":%d
                                        }
                                        """.formatted(
                                                staleVersion
                                        )
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
                                "$.type",
                                containsString(
                                        "inventory/optimistic-lock-conflict"
                                )
                        )
                );
    }

    private void ensureAdminExists() {
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
                PASSWORD_HASH,
                "Inventory",
                "Admin"
        );
    }

    private UUID productId() {
        UUID productId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT p.id
                        FROM products p
                        JOIN inventory i
                            ON i.product_id = p.id
                        WHERE p.active = TRUE
                        ORDER BY p.sku
                        LIMIT 1
                        """,
                        UUID.class
                );

        if (productId == null) {
            throw new IllegalStateException(
                    "No active product with inventory is available for inventory tests."
            );
        }

        return productId;
    }

    private Long currentVersion(
            UUID productId
    ) {
        Long version =
                jdbcTemplate.queryForObject(
                        """
                        SELECT version
                        FROM inventory
                        WHERE product_id = ?
                        """,
                        Long.class,
                        productId
                );

        if (version == null) {
            throw new IllegalStateException(
                    "Inventory version is not available for product: "
                            + productId
            );
        }

        return version;
    }

    private org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors
            .JwtRequestPostProcessor adminJwt() {

        return jwt()
                .jwt(
                        jwt ->
                                jwt
                                        .subject(
                                                ADMIN_ID.toString()
                                        )
                                        .claim(
                                                "email",
                                                ADMIN_EMAIL
                                        )
                                        .claim(
                                                "roles",
                                                List.of(
                                                        "ADMIN"
                                                )
                                        )
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_ADMIN"
                        )
                );
    }

    private org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors
            .JwtRequestPostProcessor customerJwt() {

        return jwt()
                .jwt(
                        jwt ->
                                jwt
                                        .subject(
                                                "96666666-6666-6666-6666-666666666666"
                                        )
                                        .claim(
                                                "email",
                                                "inventory.customer@launchforge.dev"
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
                );
    }

    private String validAdjustmentPayload() {
        UUID productId =
                productId();

        Long version =
                currentVersion(
                        productId
                );

        return """
                {
                  "operation":"INCREASE",
                  "quantity":2,
                  "version":%d
                }
                """.formatted(
                version
        );
    }
}

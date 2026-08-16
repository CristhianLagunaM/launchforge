package com.launchforge.catalog;

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
@SuppressWarnings("null")
class ProductControllerMockMvcTest
        extends AbstractPostgresIntegrationTest {

    private static final UUID ADMIN_ID =
            UUID.fromString(
                    "93333333-3333-3333-3333-333333333333"
            );

    private static final String ADMIN_EMAIL =
            "product.admin@launchforge.dev";

    private static final String TEST_PRODUCT_SKU =
            "LF-NEW-API-001";

    private static final String TEST_PRODUCT_SLUG =
            "api-created-product";

    private static final String PASSWORD_HASH =
            "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void resetFixtures() {
        cleanupTestProduct();
        ensureAdminExists();
    }

    @Test
    void getProductsIsPublic() throws Exception {
        mockMvc.perform(
                        get("/api/v1/products")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.content"
                        ).isArray()
                )
                .andExpect(
                        jsonPath(
                                "$.content[*].active",
                                hasItem(true)
                        )
                );
    }

    @Test
    void createProductRejectsAnonymousRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validProductPayload()
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
                );
    }

    @Test
    void createProductRejectsCustomerRole()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/products")
                                .with(
                                        customerJwt()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validProductPayload()
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
    void createProductAllowsAdminRole()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/products")
                                .with(
                                        adminJwt()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validProductPayload()
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.sku"
                        ).value(
                                TEST_PRODUCT_SKU
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.name"
                        ).value(
                                "API Created Product"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.active"
                        ).value(
                                true
                        )
                );
    }

    @Test
    void getProductReturnsNotFoundForUnknownId()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/products/99999999-9999-9999-9999-999999999999"
                        )
                )
                .andExpect(
                        status().isNotFound()
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
    void createProductValidatesRequestBody()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/products")
                                .with(
                                        adminJwt()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "sku":"",
                                          "name":"",
                                          "slug":"",
                                          "description":"",
                                          "categoryId":%d,
                                          "price":-1
                                        }
                                        """.formatted(
                                                categoryId()
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
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
                "Product",
                "Admin"
        );
    }

    private void cleanupTestProduct() {
        List<UUID> productIds =
                jdbcTemplate.query(
                        """
                        SELECT id
                        FROM products
                        WHERE sku = ?
                           OR slug = ?
                        """,
                        (resultSet, rowNum) ->
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                        TEST_PRODUCT_SKU,
                        TEST_PRODUCT_SLUG
                );

        for (UUID productId : productIds) {
            jdbcTemplate.update(
                    """
                    DELETE FROM inventory
                    WHERE product_id = ?
                    """,
                    productId
            );

            jdbcTemplate.update(
                    """
                    DELETE FROM products
                    WHERE id = ?
                    """,
                    productId
            );
        }
    }

    private Long categoryId() {
        Long categoryId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM categories
                        ORDER BY id
                        LIMIT 1
                        """,
                        Long.class
                );

        if (categoryId == null) {
            throw new IllegalStateException(
                    "No category is available for product controller tests."
            );
        }

        return categoryId;
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
                                                "94444444-4444-4444-4444-444444444444"
                                        )
                                        .claim(
                                                "email",
                                                "product.customer@launchforge.dev"
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

    private String validProductPayload() {
        return """
                {
                  "sku":"LF-NEW-API-001",
                  "name":"API Created Product",
                  "slug":"api-created-product",
                  "description":"Created through MockMvc",
                  "categoryId":%d,
                  "price":1700.00
                }
                """.formatted(
                categoryId()
        );
    }
}

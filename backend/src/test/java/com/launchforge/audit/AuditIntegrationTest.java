package com.launchforge.audit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.jayway.jsonpath.JsonPath;
import com.launchforge.audit.application.AuditAction;
import com.launchforge.audit.application.LogAction;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuditIntegrationTest.RollbackConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("null")
class AuditIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID ADMIN_ID =
            UUID.fromString(
                    "97777777-7777-7777-7777-777777777777"
            );

    private static final UUID CUSTOMER_ID =
            UUID.fromString(
                    "98888888-8888-8888-8888-888888888888"
            );

    private static final String ADMIN_EMAIL =
            "audit.admin@launchforge.dev";

    private static final String CUSTOMER_EMAIL =
            "audit.customer@launchforge.dev";

    private static final String PASSWORD_HASH =
            "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RollbackProbe rollbackProbe;

    @BeforeEach
    public void resetFixtures() {
        deleteAuditByCorrelation(
                "audit-product-update"
        );

        deleteAuditByCorrelation(
                "audit-inventory-adjust"
        );

        deleteAuditByCorrelation(
                "audit-order-cancel"
        );

        jdbcTemplate.update(
                """
                DELETE FROM audit_log
                WHERE resource_type = 'ROLLBACK_PROBE'
                """
        );

        cleanupOrder(
                "audit-cancel-order"
        );

        ensureUserExists(
                ADMIN_ID,
                ADMIN_EMAIL,
                "Audit",
                "Admin"
        );

        ensureUserExists(
                CUSTOMER_ID,
                CUSTOMER_EMAIL,
                "Audit",
                "Customer"
        );

        UUID productId =
                activeProductId();

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET
                    available_quantity = 8,
                    reserved_quantity = 0
                WHERE product_id = ?
                """,
                productId
        );
    }

    @Test
    void productUpdateCapturesActorCorrelationAndMetadata()
            throws Exception {

        ProductFixture product =
                activeProduct();

        mockMvc.perform(
                        put(
                                "/api/v1/products/{id}",
                                product.id()
                        )
                                .with(
                                        adminJwt()
                                )
                                .header(
                                        "X-Correlation-Id",
                                        "audit-product-update"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "sku":"%s",
                                          "name":"%s Audited",
                                          "slug":"%s",
                                          "description":"%s",
                                          "categoryId":%d,
                                          "price":%s
                                        }
                                        """.formatted(
                                                product.sku(),
                                                escapeJson(
                                                        product.name()
                                                ),
                                                product.slug(),
                                                escapeJson(
                                                        product.description()
                                                ),
                                                product.categoryId(),
                                                product.price()
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        Map<String, Object> row =
                auditByCorrelation(
                        "audit-product-update"
                );

        assertThat(
                row
        )
                .containsEntry(
                        "action",
                        "PRODUCT_UPDATED"
                )
                .containsEntry(
                        "actor_user_id",
                        ADMIN_ID
                );

        assertThat(
                row.get("metadata")
                        .toString()
        )
                .contains(
                        product.sku()
                )
                .doesNotContain(
                        "password",
                        "JWT",
                        "secret"
                );
    }

    @Test
    void inventoryAdjustmentCapturesExpectedMetadata()
            throws Exception {

        UUID productId =
                activeProductId();

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

        assertThat(
                version
        ).isNotNull();

        mockMvc.perform(
                        patch(
                                "/api/v1/inventory/{productId}",
                                productId
                        )
                                .with(
                                        adminJwt()
                                )
                                .header(
                                        "X-Correlation-Id",
                                        "audit-inventory-adjust"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "operation":"INCREASE",
                                          "quantity":2,
                                          "version":%d
                                        }
                                        """.formatted(
                                                version
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        String metadata =
                auditByCorrelation(
                        "audit-inventory-adjust"
                )
                        .get("metadata")
                        .toString();

        assertThat(
                metadata
        ).contains(
                "previousQuantity",
                "newQuantity",
                "INCREASE"
        );
    }

    @Test
    void cancelledOrderIsAudited()
            throws Exception {

        UUID productId =
                activeProductId();

        var created =
                mockMvc.perform(
                                post("/api/v1/orders")
                                        .with(
                                                customerJwt()
                                        )
                                        .header(
                                                "Idempotency-Key",
                                                "audit-cancel-order"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "items": [
                                                    {
                                                      "productId": "%s",
                                                      "quantity": 1
                                                    }
                                                  ],
                                                  "requirementDescription": "Necesito una solución digital para validar la auditoría.",
                                                  "projectObjective": "Comprobar el registro de cancelación de órdenes.",
                                                  "contactEmail": "%s",
                                                  "contactPhone": "+57 300 000 0000",
                                                  "referencesUrl": "https://example.com/audit-reference"
                                                }
                                                """.formatted(
                                                        productId,
                                                        CUSTOMER_EMAIL
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        String orderId =
                JsonPath.read(
                        created
                                .getResponse()
                                .getContentAsString(),
                        "$.id"
                );

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/cancel",
                                orderId
                        )
                                .with(
                                        customerJwt()
                                )
                                .header(
                                        "X-Correlation-Id",
                                        "audit-order-cancel"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        Map<String, Object> row =
                auditByCorrelation(
                        "audit-order-cancel"
                );

        assertThat(
                row
        )
                .containsEntry(
                        "action",
                        "ORDER_CANCELLED"
                )
                .containsEntry(
                        "resource_id",
                        orderId
                );

        assertThat(
                row.get("metadata")
                        .toString()
        ).contains(
                "CREATED",
                "CANCELLED"
        );
    }

    @Test
    void rollbackDoesNotLeaveSuccessAudit() {
        rollbackProbe.execute();

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM audit_log
                        WHERE resource_type = 'ROLLBACK_PROBE'
                        """,
                        Integer.class
                );

        assertThat(
                count
        ).isZero();
    }

    private void ensureUserExists(
            UUID id,
            String email,
            String firstName,
            String lastName
    ) {
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
                id,
                email,
                PASSWORD_HASH,
                firstName,
                lastName
        );
    }

    private void cleanupOrder(
            String idempotencyKey
    ) {
        jdbcTemplate.update(
                """
                DELETE FROM order_discounts
                WHERE order_id IN (
                    SELECT id
                    FROM orders
                    WHERE idempotency_key = ?
                )
                """,
                idempotencyKey
        );

        jdbcTemplate.update(
                """
                DELETE FROM order_items
                WHERE order_id IN (
                    SELECT id
                    FROM orders
                    WHERE idempotency_key = ?
                )
                """,
                idempotencyKey
        );

        jdbcTemplate.update(
                """
                DELETE FROM orders
                WHERE idempotency_key = ?
                """,
                idempotencyKey
        );
    }

    private void deleteAuditByCorrelation(
            String correlationId
    ) {
        jdbcTemplate.update(
                """
                DELETE FROM audit_log
                WHERE correlation_id = ?
                """,
                correlationId
        );
    }

    private Map<String, Object> auditByCorrelation(
            String correlationId
    ) {
        return jdbcTemplate.queryForMap(
                """
                SELECT
                    actor_user_id,
                    action,
                    resource_id,
                    metadata
                FROM audit_log
                WHERE correlation_id = ?
                """,
                correlationId
        );
    }

    private UUID activeProductId() {
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
                    "No active product with inventory is available for audit tests."
            );
        }

        return productId;
    }

    private ProductFixture activeProduct() {
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    p.id,
                    p.sku,
                    p.name,
                    p.slug,
                    p.description,
                    p.category_id,
                    p.price
                FROM products p
                JOIN inventory i
                    ON i.product_id = p.id
                WHERE p.active = TRUE
                ORDER BY p.sku
                LIMIT 1
                """,
                (resultSet, rowNum) ->
                        new ProductFixture(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                resultSet.getString(
                                        "sku"
                                ),
                                resultSet.getString(
                                        "name"
                                ),
                                resultSet.getString(
                                        "slug"
                                ),
                                resultSet.getString(
                                        "description"
                                ),
                                resultSet.getLong(
                                        "category_id"
                                ),
                                resultSet.getBigDecimal(
                                        "price"
                                )
                        )
        );
    }

    private String escapeJson(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }

    private org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors
            .JwtRequestPostProcessor adminJwt() {

        return jwt()
                .jwt(
                        token ->
                                token
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
                        token ->
                                token
                                        .subject(
                                                CUSTOMER_ID.toString()
                                        )
                                        .claim(
                                                "email",
                                                CUSTOMER_EMAIL
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

    private record ProductFixture(
            UUID id,
            String sku,
            String name,
            String slug,
            String description,
            Long categoryId,
            BigDecimal price
    ) {
    }

    @TestConfiguration
    static class RollbackConfiguration {

        @Bean
        public RollbackProbe rollbackProbe() {
            return new RollbackProbe();
        }
    }

    static class RollbackProbe {

        @Transactional
        @LogAction(
                action = AuditAction.PRODUCT_UPDATED,
                resource = "ROLLBACK_PROBE",
                resourceId = "#result"
        )
        public String execute() {
            TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();

            return "rollback";
        }
    }
}

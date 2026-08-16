package com.launchforge.discounts;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.launchforge.discounts.application.RandomProvider;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DiscountIntegrationTest.DeterministicRandomConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("null")
class DiscountIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID FREQUENT_CUSTOMER_ID =
            UUID.fromString(
                    "99111111-1111-1111-1111-111111111111"
            );

    private static final UUID CANCELLED_ONLY_CUSTOMER_ID =
            UUID.fromString(
                    "99222222-2222-2222-2222-222222222222"
            );

    private static final String FREQUENT_CUSTOMER_EMAIL =
            "discount.frequent@launchforge.dev";

    private static final String CANCELLED_ONLY_CUSTOMER_EMAIL =
            "discount.cancelled@launchforge.dev";

    private static final String PASSWORD_HASH =
            "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void resetFixtures() {
        ensureCustomerExists(
                FREQUENT_CUSTOMER_ID,
                FREQUENT_CUSTOMER_EMAIL,
                "Frequent",
                "Customer"
        );

        ensureCustomerExists(
                CANCELLED_ONLY_CUSTOMER_ID,
                CANCELLED_ONLY_CUSTOMER_EMAIL,
                "Cancelled",
                "Customer"
        );

        cleanupTestOrders();

        resetDiscountConfiguration();

        UUID productId =
                activeProductId();

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET
                    available_quantity = 20,
                    reserved_quantity = 0
                WHERE product_id = ?
                """,
                productId
        );
    }

    @Test
    void loadsDiscountConfigurationFromDatabase() {
        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        """
                        SELECT code, enabled, percentage
                        FROM discount_configuration
                        ORDER BY code
                        """
                );

        assertThat(
                rows
        ).hasSize(
                3
        );

        assertThat(
                rows
        )
                .extracting(
                        row ->
                                row.get("code")
                )
                .containsExactly(
                        "FREQUENT_CUSTOMER",
                        "RANDOM_ORDER",
                        "TIME_RANGE"
                );
    }

    @Test
    void creatingOrderPersistsAccumulativeDiscountBreakdownForFrequentCustomer()
            throws Exception {

        createHistoricalCompletedOrders(
                FREQUENT_CUSTOMER_ID,
                5
        );

        alignTimeBoundDiscountsWithNow();

        var response =
                mockMvc.perform(
                                post("/api/v1/orders")
                                        .with(
                                                customerJwt(
                                                        FREQUENT_CUSTOMER_ID,
                                                        FREQUENT_CUSTOMER_EMAIL
                                                )
                                        )
                                        .header(
                                                "Idempotency-Key",
                                                "discount-it-001"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                singleProductPayload(
                                                        FREQUENT_CUSTOMER_EMAIL
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.discounts.length()"
                                ).value(
                                        3
                                )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.discounts[0].code"
                                ).value(
                                        "TIME_RANGE"
                                )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.discounts[1].code"
                                ).value(
                                        "RANDOM_ORDER"
                                )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.discounts[2].code"
                                ).value(
                                        "FREQUENT_CUSTOMER"
                                )
                        )
                        .andReturn();

        String orderId =
                JsonPath.read(
                        response
                                .getResponse()
                                .getContentAsString(),
                        "$.id"
                );

        BigDecimal subtotal =
                new BigDecimal(
                        JsonPath.read(
                                response
                                        .getResponse()
                                        .getContentAsString(),
                                "$.subtotal"
                        )
                                .toString()
                );

        BigDecimal expectedTimeRange =
                subtotal
                        .multiply(
                                new BigDecimal("0.10")
                        )
                        .setScale(
                                2
                        );

        BigDecimal expectedRandom =
                subtotal
                        .multiply(
                                new BigDecimal("0.50")
                        )
                        .setScale(
                                2
                        );

        BigDecimal expectedFrequent =
                subtotal
                        .multiply(
                                new BigDecimal("0.05")
                        )
                        .setScale(
                                2
                        );

        BigDecimal expectedDiscountTotal =
                expectedTimeRange
                        .add(
                                expectedRandom
                        )
                        .add(
                                expectedFrequent
                        );

        BigDecimal expectedTotal =
                subtotal.subtract(
                        expectedDiscountTotal
                );

        BigDecimal actualDiscountTotal =
                new BigDecimal(
                        JsonPath.read(
                                response
                                        .getResponse()
                                        .getContentAsString(),
                                "$.discountTotal"
                        )
                                .toString()
                );

        BigDecimal actualTotal =
                new BigDecimal(
                        JsonPath.read(
                                response
                                        .getResponse()
                                        .getContentAsString(),
                                "$.total"
                        )
                                .toString()
                );

        assertThat(
                actualDiscountTotal
        ).isEqualByComparingTo(
                expectedDiscountTotal
        );

        assertThat(
                actualTotal
        ).isEqualByComparingTo(
                expectedTotal
        );

        List<Map<String, Object>> persistedDiscounts =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            code,
                            percentage,
                            base_amount,
                            amount,
                            application_order
                        FROM order_discounts
                        WHERE order_id = ?::uuid
                        ORDER BY application_order
                        """,
                        orderId
                );

        assertThat(
                persistedDiscounts
        ).hasSize(
                3
        );

        assertThat(
                persistedDiscounts.get(0)
        )
                .containsEntry(
                        "code",
                        "TIME_RANGE"
                )
                .containsEntry(
                        "application_order",
                        1
                );

        assertThat(
                persistedDiscounts.get(1)
        )
                .containsEntry(
                        "code",
                        "RANDOM_ORDER"
                )
                .containsEntry(
                        "application_order",
                        2
                );

        assertThat(
                persistedDiscounts.get(2)
        )
                .containsEntry(
                        "code",
                        "FREQUENT_CUSTOMER"
                )
                .containsEntry(
                        "application_order",
                        3
                );
    }

    @Test
    void cancelledOrdersDoNotCountTowardFrequentCustomerDiscount()
            throws Exception {

        disableTimeBoundDiscounts();

        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET
                    enabled = TRUE,
                    percentage = 5.00,
                    minimum_orders = 1,
                    lookback_months = 24
                WHERE code = 'FREQUENT_CUSTOMER'
                """
        );

        createHistoricalCancelledOrder(
                CANCELLED_ONLY_CUSTOMER_ID
        );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(
                                        customerJwt(
                                                CANCELLED_ONLY_CUSTOMER_ID,
                                                CANCELLED_ONLY_CUSTOMER_EMAIL
                                        )
                                )
                                .header(
                                        "Idempotency-Key",
                                        "discount-it-002"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        singleProductPayload(
                                                CANCELLED_ONLY_CUSTOMER_EMAIL
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.discountTotal"
                        ).value(
                                0.0
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.discounts.length()"
                        ).value(
                                0
                        )
                );
    }

    @Test
    void historicalOrdersKeepTheirDiscountTraceAfterConfigurationChanges() {
        UUID historicalOrderId =
                createHistoricalOrderWithDiscountTrace();

        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET
                    percentage = 90.00,
                    start_at = ?,
                    end_at = ?
                WHERE code IN (
                    'TIME_RANGE',
                    'RANDOM_ORDER'
                )
                """,
                Timestamp.from(
                        Instant.parse(
                                "2026-08-01T00:00:00Z"
                        )
                ),
                Timestamp.from(
                        Instant.parse(
                                "2026-08-31T23:59:59Z"
                        )
                )
        );

        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET
                    percentage = 90.00,
                    minimum_orders = 1,
                    lookback_months = 1
                WHERE code = 'FREQUENT_CUSTOMER'
                """
        );

        List<Map<String, Object>> persistedDiscounts =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            code,
                            percentage,
                            amount,
                            application_order
                        FROM order_discounts
                        WHERE order_id = ?
                        ORDER BY application_order
                        """,
                        historicalOrderId
                );

        assertThat(
                persistedDiscounts
        ).hasSize(
                3
        );

        assertThat(
                persistedDiscounts.get(0)
        )
                .containsEntry(
                        "code",
                        "TIME_RANGE"
                );

        assertThat(
                persistedDiscounts.get(0)
                        .get("percentage")
        ).isEqualTo(
                new BigDecimal("10.00")
        );

        assertThat(
                persistedDiscounts.get(0)
                        .get("amount")
        ).isEqualTo(
                new BigDecimal("120.00")
        );

        assertThat(
                persistedDiscounts.get(1)
        )
                .containsEntry(
                        "code",
                        "RANDOM_ORDER"
                );

        assertThat(
                persistedDiscounts.get(1)
                        .get("percentage")
        ).isEqualTo(
                new BigDecimal("50.00")
        );

        assertThat(
                persistedDiscounts.get(1)
                        .get("amount")
        ).isEqualTo(
                new BigDecimal("600.00")
        );

        assertThat(
                persistedDiscounts.get(2)
        )
                .containsEntry(
                        "code",
                        "FREQUENT_CUSTOMER"
                );

        assertThat(
                persistedDiscounts.get(2)
                        .get("percentage")
        ).isEqualTo(
                new BigDecimal("5.00")
        );

        assertThat(
                persistedDiscounts.get(2)
                        .get("amount")
        ).isEqualTo(
                new BigDecimal("60.00")
        );
    }

    private void ensureCustomerExists(
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

    private void cleanupTestOrders() {
        jdbcTemplate.update(
                """
                DELETE FROM order_discounts
                WHERE order_id IN (
                    SELECT id
                    FROM orders
                    WHERE idempotency_key LIKE 'discount-it-%'
                       OR idempotency_key LIKE 'discount-history-%'
                )
                """
        );

        jdbcTemplate.update(
                """
                DELETE FROM order_items
                WHERE order_id IN (
                    SELECT id
                    FROM orders
                    WHERE idempotency_key LIKE 'discount-it-%'
                       OR idempotency_key LIKE 'discount-history-%'
                )
                """
        );

        jdbcTemplate.update(
                """
                DELETE FROM orders
                WHERE idempotency_key LIKE 'discount-it-%'
                   OR idempotency_key LIKE 'discount-history-%'
                """
        );
    }

    private void resetDiscountConfiguration() {
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET
                    enabled = FALSE,
                    percentage = CASE
                        WHEN code = 'TIME_RANGE' THEN 10.00
                        WHEN code = 'RANDOM_ORDER' THEN 50.00
                        WHEN code = 'FREQUENT_CUSTOMER' THEN 5.00
                    END,
                    start_at = NULL,
                    end_at = NULL,
                    minimum_orders = CASE
                        WHEN code = 'FREQUENT_CUSTOMER' THEN 5
                        ELSE NULL
                    END,
                    lookback_months = CASE
                        WHEN code = 'FREQUENT_CUSTOMER' THEN 12
                        ELSE NULL
                    END,
                    updated_at = NOW()
                """
        );
    }

    private void createHistoricalCompletedOrders(
            UUID customerId,
            int count
    ) {
        UUID productId =
                activeProductId();

        BigDecimal price =
                productPrice(
                        productId
                );

        for (int index = 0; index < count; index++) {
            UUID orderId =
                    UUID.randomUUID();

            jdbcTemplate.update(
                    """
                    INSERT INTO orders (
                        id,
                        order_number,
                        customer_id,
                        status,
                        subtotal,
                        discount_total,
                        total,
                        idempotency_key,
                        requirement_description,
                        project_objective,
                        contact_email,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        ?,
                        ?,
                        ?,
                        'COMPLETED',
                        ?,
                        0.00,
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        NOW() - INTERVAL '1 month',
                        NOW() - INTERVAL '1 month'
                    )
                    """,
                    orderId,
                    "LF-DISCOUNT-HISTORY-" + index,
                    customerId,
                    price,
                    price,
                    "discount-history-completed-" + index,
                    "Historical completed order for discount testing.",
                    "Validate frequent customer eligibility.",
                    FREQUENT_CUSTOMER_EMAIL
            );

            jdbcTemplate.update(
                    """
                    INSERT INTO order_items (
                        id,
                        order_id,
                        product_id,
                        product_name,
                        sku,
                        quantity,
                        unit_price,
                        subtotal
                    )
                    SELECT
                        ?,
                        ?,
                        p.id,
                        p.name,
                        p.sku,
                        1,
                        p.price,
                        p.price
                    FROM products p
                    WHERE p.id = ?
                    """,
                    UUID.randomUUID(),
                    orderId,
                    productId
            );
        }
    }

    private void createHistoricalCancelledOrder(
            UUID customerId
    ) {
        UUID productId =
                activeProductId();

        BigDecimal price =
                productPrice(
                        productId
                );

        UUID orderId =
                UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO orders (
                    id,
                    order_number,
                    customer_id,
                    status,
                    subtotal,
                    discount_total,
                    total,
                    idempotency_key,
                    requirement_description,
                    project_objective,
                    contact_email,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    'CANCELLED',
                    ?,
                    0.00,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    NOW() - INTERVAL '1 month',
                    NOW() - INTERVAL '1 month'
                )
                """,
                orderId,
                "LF-DISCOUNT-CANCELLED",
                customerId,
                price,
                price,
                "discount-history-cancelled",
                "Historical cancelled order for discount testing.",
                "Ensure cancelled orders do not count.",
                CANCELLED_ONLY_CUSTOMER_EMAIL
        );

        jdbcTemplate.update(
                """
                INSERT INTO order_items (
                    id,
                    order_id,
                    product_id,
                    product_name,
                    sku,
                    quantity,
                    unit_price,
                    subtotal
                )
                SELECT
                    ?,
                    ?,
                    p.id,
                    p.name,
                    p.sku,
                    1,
                    p.price,
                    p.price
                FROM products p
                WHERE p.id = ?
                """,
                UUID.randomUUID(),
                orderId,
                productId
        );
    }

    private UUID createHistoricalOrderWithDiscountTrace() {
        UUID productId =
                activeProductId();

        BigDecimal price =
                productPrice(
                        productId
                );

        UUID orderId =
                UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO orders (
                    id,
                    order_number,
                    customer_id,
                    status,
                    subtotal,
                    discount_total,
                    total,
                    idempotency_key,
                    requirement_description,
                    project_objective,
                    contact_email,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    'COMPLETED',
                    ?,
                    780.00,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    NOW() - INTERVAL '1 month',
                    NOW() - INTERVAL '1 month'
                )
                """,
                orderId,
                "LF-DISCOUNT-TRACE",
                FREQUENT_CUSTOMER_ID,
                price,
                price.subtract(
                        new BigDecimal("780.00")
                ),
                "discount-history-trace",
                "Historical order with persisted discount trace.",
                "Validate historical discount immutability.",
                FREQUENT_CUSTOMER_EMAIL
        );

        jdbcTemplate.update(
                """
                INSERT INTO order_items (
                    id,
                    order_id,
                    product_id,
                    product_name,
                    sku,
                    quantity,
                    unit_price,
                    subtotal
                )
                SELECT
                    ?,
                    ?,
                    p.id,
                    p.name,
                    p.sku,
                    1,
                    p.price,
                    p.price
                FROM products p
                WHERE p.id = ?
                """,
                UUID.randomUUID(),
                orderId,
                productId
        );

        insertHistoricalDiscount(
                orderId,
                "TIME_RANGE",
                new BigDecimal("10.00"),
                price,
                new BigDecimal("120.00"),
                1
        );

        insertHistoricalDiscount(
                orderId,
                "RANDOM_ORDER",
                new BigDecimal("50.00"),
                price,
                new BigDecimal("600.00"),
                2
        );

        insertHistoricalDiscount(
                orderId,
                "FREQUENT_CUSTOMER",
                new BigDecimal("5.00"),
                price,
                new BigDecimal("60.00"),
                3
        );

        return orderId;
    }

    private void insertHistoricalDiscount(
            UUID orderId,
            String code,
            BigDecimal percentage,
            BigDecimal baseAmount,
            BigDecimal amount,
            int applicationOrder
    ) {
        UUID configurationId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM discount_configuration
                        WHERE code = ?
                        """,
                        UUID.class,
                        code
                );

        jdbcTemplate.update(
                """
                INSERT INTO order_discounts (
                    id,
                    order_id,
                    discount_configuration_id,
                    code,
                    percentage,
                    base_amount,
                    amount,
                    reason,
                    application_order
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                orderId,
                configurationId,
                code,
                percentage,
                baseAmount,
                amount,
                "Historical discount test fixture",
                applicationOrder
        );
    }

    private void alignTimeBoundDiscountsWithNow() {
        Instant now =
                Instant.now();

        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET
                    enabled = TRUE,
                    percentage = 10.00,
                    start_at = ?,
                    end_at = ?
                WHERE code = 'TIME_RANGE'
                """,
                Timestamp.from(
                        now.minusSeconds(
                                3600
                        )
                ),
                Timestamp.from(
                        now.plusSeconds(
                                3600
                        )
                )
        );

        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET
                    enabled = TRUE,
                    percentage = 50.00,
                    start_at = ?,
                    end_at = ?
                WHERE code = 'RANDOM_ORDER'
                """,
                Timestamp.from(
                        now.minusSeconds(
                                3600
                        )
                ),
                Timestamp.from(
                        now.plusSeconds(
                                3600
                        )
                )
        );

        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET
                    enabled = TRUE,
                    percentage = 5.00,
                    minimum_orders = 5,
                    lookback_months = 12
                WHERE code = 'FREQUENT_CUSTOMER'
                """
        );
    }

    private void disableTimeBoundDiscounts() {
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET enabled = FALSE
                WHERE code IN (
                    'TIME_RANGE',
                    'RANDOM_ORDER'
                )
                """
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
                    "No active product with inventory is available for discount tests."
            );
        }

        return productId;
    }

    private BigDecimal productPrice(
            UUID productId
    ) {
        BigDecimal price =
                jdbcTemplate.queryForObject(
                        """
                        SELECT price
                        FROM products
                        WHERE id = ?
                        """,
                        BigDecimal.class,
                        productId
                );

        if (price == null) {
            throw new IllegalStateException(
                    "Product price is not available for: "
                            + productId
            );
        }

        return price;
    }

    private org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors
            .JwtRequestPostProcessor customerJwt(
                    UUID customerId,
                    String email
            ) {

        return jwt()
                .jwt(
                        jwt ->
                                jwt
                                        .subject(
                                                customerId.toString()
                                        )
                                        .claim(
                                                "email",
                                                email
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

    private String singleProductPayload(
            String contactEmail
    ) {
        return """
                {
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 1
                    }
                  ],
                  "requirementDescription": "Necesito una solución digital para validar descuentos.",
                  "projectObjective": "Comprobar la aplicación acumulativa de descuentos.",
                  "contactEmail": "%s",
                  "contactPhone": "+57 300 000 0000",
                  "referencesUrl": "https://example.com/discount-reference"
                }
                """.formatted(
                activeProductId(),
                contactEmail
        );
    }

    @TestConfiguration
    static class DeterministicRandomConfiguration {

        @Bean
        @Primary
        public RandomProvider randomProvider() {
            return (
                    orderId,
                    customerId
            ) -> true;
        }
    }
}

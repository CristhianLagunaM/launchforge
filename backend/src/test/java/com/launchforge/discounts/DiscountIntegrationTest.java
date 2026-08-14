package com.launchforge.discounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.launchforge.discounts.application.RandomProvider;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DiscountIntegrationTest.DeterministicRandomConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DiscountIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID FREQUENT_CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111113");
    private static final UUID CANCELLED_ONLY_CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111115");
    private static final UUID HISTORICAL_ORDER_ID = UUID.fromString("44444444-4444-4444-4444-444444444406");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsDiscountConfigurationFromDatabase() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT code, enabled, percentage
                FROM discount_configuration
                ORDER BY code
                """);

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(row -> row.get("code"))
                .containsExactly("FREQUENT_CUSTOMER", "RANDOM_ORDER", "TIME_RANGE");
    }

    @Test
    void creatingOrderPersistsAccumulativeDiscountBreakdownForFrequentCustomer() throws Exception {
        alignTimeBoundDiscountsWithNow();

        var response = mockMvc.perform(post("/api/v1/orders")
                        .with(customerJwt(FREQUENT_CUSTOMER_ID, "frequent@launchforge.dev"))
                        .header("Idempotency-Key", "discount-it-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleProductPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountTotal").value(780.0))
                .andExpect(jsonPath("$.total").value(420.0))
                .andExpect(jsonPath("$.discounts.length()").value(3))
                .andExpect(jsonPath("$.discounts[0].code").value("TIME_RANGE"))
                .andExpect(jsonPath("$.discounts[1].code").value("RANDOM_ORDER"))
                .andExpect(jsonPath("$.discounts[2].code").value("FREQUENT_CUSTOMER"))
                .andReturn();

        String orderId = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        List<Map<String, Object>> persistedDiscounts = jdbcTemplate.queryForList(
                """
                SELECT code, percentage, base_amount, amount, application_order
                FROM order_discounts
                WHERE order_id = ?::uuid
                ORDER BY application_order
                """,
                orderId
        );

        assertThat(persistedDiscounts).hasSize(3);
        assertThat(persistedDiscounts.get(0)).containsEntry("code", "TIME_RANGE").containsEntry("application_order", 1);
        assertThat(persistedDiscounts.get(1)).containsEntry("code", "RANDOM_ORDER").containsEntry("application_order", 2);
        assertThat(persistedDiscounts.get(2)).containsEntry("code", "FREQUENT_CUSTOMER").containsEntry("application_order", 3);
        assertThat(persistedDiscounts.get(0).get("amount")).isEqualTo(new java.math.BigDecimal("120.00"));
        assertThat(persistedDiscounts.get(1).get("amount")).isEqualTo(new java.math.BigDecimal("600.00"));
        assertThat(persistedDiscounts.get(2).get("amount")).isEqualTo(new java.math.BigDecimal("60.00"));
    }

    @Test
    void cancelledOrdersDoNotCountTowardFrequentCustomerDiscount() throws Exception {
        disableTimeBoundDiscounts();
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET enabled = TRUE,
                    percentage = 5.00,
                    minimum_orders = 1,
                    lookback_months = 24
                WHERE code = 'FREQUENT_CUSTOMER'
                """
        );

        mockMvc.perform(post("/api/v1/orders")
                        .with(customerJwt(CANCELLED_ONLY_CUSTOMER_ID, "oscar@launchforge.dev"))
                        .header("Idempotency-Key", "discount-it-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleProductPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountTotal").value(0.0))
                .andExpect(jsonPath("$.discounts.length()").value(0));
    }

    @Test
    void historicalOrdersKeepTheirDiscountTraceAfterConfigurationChanges() {
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET percentage = 90.00,
                    start_at = ?,
                    end_at = ?
                WHERE code IN ('TIME_RANGE', 'RANDOM_ORDER')
                """,
                Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2026-08-31T23:59:59Z"))
        );
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET percentage = 90.00,
                    minimum_orders = 1,
                    lookback_months = 1
                WHERE code = 'FREQUENT_CUSTOMER'
                """
        );

        List<Map<String, Object>> persistedDiscounts = jdbcTemplate.queryForList(
                """
                SELECT code, percentage, amount, application_order
                FROM order_discounts
                WHERE order_id = ?::uuid
                ORDER BY application_order
                """,
                HISTORICAL_ORDER_ID.toString()
        );

        assertThat(persistedDiscounts).hasSize(3);
        assertThat(persistedDiscounts.get(0)).containsEntry("code", "TIME_RANGE");
        assertThat(persistedDiscounts.get(0).get("percentage")).isEqualTo(new java.math.BigDecimal("10.00"));
        assertThat(persistedDiscounts.get(0).get("amount")).isEqualTo(new java.math.BigDecimal("810.00"));
        assertThat(persistedDiscounts.get(1)).containsEntry("code", "RANDOM_ORDER");
        assertThat(persistedDiscounts.get(1).get("percentage")).isEqualTo(new java.math.BigDecimal("50.00"));
        assertThat(persistedDiscounts.get(1).get("amount")).isEqualTo(new java.math.BigDecimal("4050.00"));
        assertThat(persistedDiscounts.get(2)).containsEntry("code", "FREQUENT_CUSTOMER");
        assertThat(persistedDiscounts.get(2).get("percentage")).isEqualTo(new java.math.BigDecimal("5.00"));
        assertThat(persistedDiscounts.get(2).get("amount")).isEqualTo(new java.math.BigDecimal("405.00"));
    }

    private void alignTimeBoundDiscountsWithNow() {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET enabled = TRUE,
                    percentage = 10.00,
                    start_at = ?,
                    end_at = ?
                WHERE code = 'TIME_RANGE'
                """,
                Timestamp.from(now.minusSeconds(3600)),
                Timestamp.from(now.plusSeconds(3600))
        );
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET enabled = TRUE,
                    percentage = 50.00,
                    start_at = ?,
                    end_at = ?
                WHERE code = 'RANDOM_ORDER'
                """,
                Timestamp.from(now.minusSeconds(3600)),
                Timestamp.from(now.plusSeconds(3600))
        );
        jdbcTemplate.update(
                """
                UPDATE discount_configuration
                SET enabled = TRUE,
                    percentage = 5.00,
                    minimum_orders = 5,
                    lookback_months = 12
                WHERE code = 'FREQUENT_CUSTOMER'
                """
        );
    }

    private void disableTimeBoundDiscounts() {
        jdbcTemplate.update("UPDATE discount_configuration SET enabled = FALSE WHERE code IN ('TIME_RANGE', 'RANDOM_ORDER')");
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt(
            UUID customerId,
            String email
    ) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(customerId.toString())
                        .claim("email", email)
                        .claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private String singleProductPayload() {
        return """
                {
                  "items": [
                    {
                      "productId": "22222222-2222-2222-2222-222222222221",
                      "quantity": 1
                    }
                  ]
                }
                """;
    }

    @TestConfiguration
    static class DeterministicRandomConfiguration {

        @Bean
        @Primary
        RandomProvider randomProvider() {
            return (orderId, customerId) -> true;
        }
    }
}

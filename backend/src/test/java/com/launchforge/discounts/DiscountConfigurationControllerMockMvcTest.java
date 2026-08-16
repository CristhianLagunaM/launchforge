package com.launchforge.discounts;

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
class DiscountConfigurationControllerMockMvcTest
                extends AbstractPostgresIntegrationTest {

        private static final UUID ADMIN_ID = UUID.fromString(
                        "99333333-3333-3333-3333-333333333333");

        private static final String ADMIN_EMAIL = "discount.admin@launchforge.dev";

        private static final String PASSWORD_HASH = "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @BeforeEach
        void resetFixtures() {
                ensureAdminExists();
                resetTimeRangeConfiguration();
        }

        @Test
        void customerCannotModifyDiscountConfiguration()
                        throws Exception {

                mockMvc.perform(
                                patch(
                                                "/api/v1/discount-configurations/TIME_RANGE")
                                                .with(
                                                                customerJwt())
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(
                                                                validPayload()))
                                .andExpect(
                                                status().isForbidden())
                                .andExpect(
                                                header().string(
                                                                "Content-Type",
                                                                containsString(
                                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
        }

        @Test
        void adminCanModifyDiscountConfiguration()
                        throws Exception {

                mockMvc.perform(
                                patch(
                                                "/api/v1/discount-configurations/TIME_RANGE")
                                                .with(
                                                                adminJwt())
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(
                                                                validPayload()))
                                .andExpect(
                                                status().isOk())
                                .andExpect(
                                                jsonPath(
                                                                "$.code",
                                                                is("TIME_RANGE")))
                                .andExpect(
                                                jsonPath(
                                                                "$.enabled",
                                                                is(true)))
                                .andExpect(
                                                jsonPath(
                                                                "$.percentage",
                                                                is(12.5)))
                                .andExpect(
                                                jsonPath(
                                                                "$.updatedBy",
                                                                is(
                                                                                ADMIN_ID.toString())));
        }

        @Test
        void adminCanListDiscountConfigurations()
                        throws Exception {

                mockMvc.perform(
                                get(
                                                "/api/v1/discount-configurations")
                                                .with(
                                                                adminJwt()))
                                .andExpect(
                                                status().isOk())
                                .andExpect(
                                                jsonPath(
                                                                "$[0].code").exists());
        }

        @Test
        void invalidConfigurationReturnsBadRequest()
                        throws Exception {

                mockMvc.perform(
                                patch(
                                                "/api/v1/discount-configurations/TIME_RANGE")
                                                .with(
                                                                adminJwt())
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(
                                                                """
                                                                                {
                                                                                  "enabled": true,
                                                                                  "percentage": 10.00,
                                                                                  "startAt": "2026-08-31T23:59:59Z",
                                                                                  "endAt": "2026-08-01T00:00:00Z",
                                                                                  "minimumOrders": null,
                                                                                  "lookbackMonths": null
                                                                                }
                                                                                """))
                                .andExpect(
                                                status().isBadRequest())
                                .andExpect(
                                                jsonPath(
                                                                "$.type",
                                                                containsString(
                                                                                "discounts/invalid-configuration")));
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
                                "Discount",
                                "Admin");
        }

        private void resetTimeRangeConfiguration() {
                jdbcTemplate.update(
                                """
                                                UPDATE discount_configuration
                                                SET
                                                    enabled = FALSE,
                                                    percentage = 10.00,
                                                    start_at = NULL,
                                                    end_at = NULL,
                                                    minimum_orders = NULL,
                                                    lookback_months = NULL,
                                                    updated_by = NULL,
                                                    updated_at = NOW()
                                                WHERE code = 'TIME_RANGE'
                                                """);
        }

        private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {

                return jwt()
                                .jwt(
                                                jwt -> jwt
                                                                .subject(
                                                                                ADMIN_ID.toString())
                                                                .claim(
                                                                                "email",
                                                                                ADMIN_EMAIL)
                                                                .claim(
                                                                                "roles",
                                                                                List.of(
                                                                                                "ADMIN")))
                                .authorities(
                                                new SimpleGrantedAuthority(
                                                                "ROLE_ADMIN"));
        }

        private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {

                return jwt()
                                .jwt(
                                                jwt -> jwt
                                                                .subject(
                                                                                "99444444-4444-4444-4444-444444444444")
                                                                .claim(
                                                                                "email",
                                                                                "discount.customer@launchforge.dev")
                                                                .claim(
                                                                                "roles",
                                                                                List.of(
                                                                                                "CUSTOMER")))
                                .authorities(
                                                new SimpleGrantedAuthority(
                                                                "ROLE_CUSTOMER"));
        }

        private String validPayload() {
                return """
                                {
                                  "enabled": true,
                                  "percentage": 12.50,
                                  "startAt": "2026-08-01T00:00:00Z",
                                  "endAt": "2026-08-31T23:59:59Z",
                                  "minimumOrders": null,
                                  "lookbackMonths": null
                                }
                                """;
        }
}

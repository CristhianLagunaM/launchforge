package com.launchforge.report;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerMockMvcTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestReturnsUnauthorized()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/reports/top-products"))
                .andExpect(
                        status().isUnauthorized());
    }

    @Test
    void customerRequestReturnsForbidden()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/reports/top-products")
                        .with(
                                customerJwt()))
                .andExpect(
                        status().isForbidden());
    }

    @Test
    void adminCanReadTopProductsReportOnCleanBaseline()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/reports/top-products")
                        .with(
                                adminJwt()))
                .andExpect(
                        status().isOk())
                .andExpect(
                        jsonPath(
                                "$").isArray())
                .andExpect(
                        jsonPath(
                                "$",
                                hasSize(0)));
    }

    @Test
    void adminCanReadDashboardMetrics()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/reports/dashboard")
                        .with(
                                adminJwt()))
                .andExpect(
                        status().isOk())
                .andExpect(
                        jsonPath(
                                "$.netRevenue").isNumber())
                .andExpect(
                        jsonPath(
                                "$.ordersByStatus.pending").isNumber())
                .andExpect(
                        jsonPath(
                                "$.capacity.available").isNumber())
                .andExpect(
                        jsonPath(
                                "$.monthlyRevenue",
                                hasSize(6)))
                .andExpect(
                        jsonPath(
                                "$.generatedAt").exists());
    }

    private RequestPostProcessor adminJwt() {
        return jwt()
                .jwt(
                        token -> token.claim(
                                "roles",
                                List.of(
                                        "ADMIN")))
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_ADMIN"));
    }

    private RequestPostProcessor customerJwt() {
        return jwt()
                .jwt(
                        token -> token.claim(
                                "roles",
                                List.of(
                                        "CUSTOMER")))
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_CUSTOMER"));
    }
}

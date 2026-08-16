package com.launchforge.report;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerRequestReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products").with(customerJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadPreparedReportResponse() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].productId").exists())
                .andExpect(jsonPath("$[0].sku").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].quantitySold").isNumber());
    }

    @Test
    void adminCanReadDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/reports/dashboard").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netRevenue").isNumber())
                .andExpect(jsonPath("$.ordersByStatus.pending").isNumber())
                .andExpect(jsonPath("$.capacity.available").isNumber())
                .andExpect(jsonPath("$.monthlyRevenue", hasSize(6)))
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(token -> token.claim("roles", List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        return jwt().jwt(token -> token.claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}

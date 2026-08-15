package com.launchforge.audit;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditControllerMockMvcTest extends AbstractPostgresIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void adminCanFilterAndPageAuditLog() throws Exception {
        mockMvc.perform(get("/api/v1/audit?action=PRODUCT_CREATED&resourceType=PRODUCT&actor=admin&page=0&size=1")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.content[*].action", everyItem(is("PRODUCT_CREATED"))))
                .andExpect(jsonPath("$.content[*].resourceType", everyItem(is("PRODUCT"))));
    }

    @Test
    void customerCannotReadAuditLog() throws Exception {
        mockMvc.perform(get("/api/v1/audit").with(customerJwt())).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadAuditLog() throws Exception {
        mockMvc.perform(get("/api/v1/audit")).andExpect(status().isUnauthorized());
    }

    @Test
    void validCorrelationIdIsPropagated() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .header("X-Correlation-Id", "audit-request_1")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "audit-request_1"));
    }

    @Test
    void invalidCorrelationIdIsReplaced() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .header("X-Correlation-Id", "invalid correlation id")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().jwt(token -> token.subject("11111111-1111-1111-1111-111111111111")
                        .claim("email", "admin@launchforge.dev").claim("roles", java.util.List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return jwt().jwt(token -> token.subject("11111111-1111-1111-1111-111111111112")
                        .claim("email", "customer@launchforge.dev").claim("roles", java.util.List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}

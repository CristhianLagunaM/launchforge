package com.launchforge.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.launchforge.audit.application.AuditAction;
import com.launchforge.audit.application.LogAction;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuditIntegrationTest.RollbackConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditIntegrationTest extends AbstractPostgresIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RollbackProbe rollbackProbe;

    @Test
    void productUpdateCapturesActorCorrelationAndMetadata() throws Exception {
        mockMvc.perform(put("/api/v1/products/22222222-2222-2222-2222-222222222221")
                        .with(adminJwt()).header("X-Correlation-Id", "audit-product-update")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"LF-LANDING-001","name":"Landing Page Audited","slug":"landing-page-launch",
                         "description":"High-conversion landing page.","categoryId":1,"price":1200.00}
                        """))
                .andExpect(status().isOk());

        Map<String, Object> row = auditByCorrelation("audit-product-update");
        assertThat(row).containsEntry("action", "PRODUCT_UPDATED")
                .containsEntry("actor_user_id", java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(row.get("metadata").toString()).contains("LF-LANDING-001").doesNotContain("password", "JWT", "secret");
    }

    @Test
    void inventoryAdjustmentCapturesExpectedMetadata() throws Exception {
        Long version = jdbcTemplate.queryForObject(
                "SELECT version FROM inventory WHERE product_id='22222222-2222-2222-2222-222222222222'", Long.class);
        mockMvc.perform(patch("/api/v1/inventory/22222222-2222-2222-2222-222222222222")
                        .with(adminJwt()).header("X-Correlation-Id", "audit-inventory-adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"INCREASE\",\"quantity\":2,\"version\":" + version + "}"))
                .andExpect(status().isOk());

        String metadata = auditByCorrelation("audit-inventory-adjust").get("metadata").toString();
        assertThat(metadata).contains("previousQuantity", "newQuantity", "INCREASE");
    }

    @Test
    void cancelledOrderIsAudited() throws Exception {
        var created = mockMvc.perform(post("/api/v1/orders").with(customerJwt())
                        .header("Idempotency-Key", "audit-cancel-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"22222222-2222-2222-2222-222222222221\",\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        String orderId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch("/api/v1/orders/{id}/cancel", orderId).with(customerJwt())
                        .header("X-Correlation-Id", "audit-order-cancel"))
                .andExpect(status().isOk());

        Map<String, Object> row = auditByCorrelation("audit-order-cancel");
        assertThat(row).containsEntry("action", "ORDER_CANCELLED").containsEntry("resource_id", orderId);
        assertThat(row.get("metadata").toString()).contains("CONFIRMED", "CANCELLED");
    }

    @Test
    void rollbackDoesNotLeaveSuccessAudit() {
        rollbackProbe.execute();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE resource_type='ROLLBACK_PROBE'", Integer.class);
        assertThat(count).isZero();
    }

    private Map<String, Object> auditByCorrelation(String correlationId) {
        return jdbcTemplate.queryForMap("SELECT actor_user_id, action, resource_id, metadata FROM audit_log WHERE correlation_id=?", correlationId);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().jwt(token -> token.subject("11111111-1111-1111-1111-111111111111")
                        .claim("email", "admin@launchforge.dev").claim("roles", List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return jwt().jwt(token -> token.subject("11111111-1111-1111-1111-111111111112")
                        .claim("email", "customer@launchforge.dev").claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @TestConfiguration
    static class RollbackConfiguration {
        @Bean RollbackProbe rollbackProbe() { return new RollbackProbe(); }
    }

    static class RollbackProbe {
        @Transactional
        @LogAction(action = AuditAction.PRODUCT_UPDATED, resource = "ROLLBACK_PROBE", resourceId = "#result")
        public String execute() {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "rollback";
        }
    }
}

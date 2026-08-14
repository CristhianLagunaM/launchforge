package com.launchforge.inventory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventoryControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getInventoryRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void patchInventoryRejectsCustomerRole() throws Exception {
        mockMvc.perform(patch("/api/v1/inventory/22222222-2222-2222-2222-222222222221")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111112")
                                        .claim("email", "customer@launchforge.dev")
                                        .claim("roles", java.util.List.of("CUSTOMER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAdjustmentPayload()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void patchInventoryAllowsAdminRole() throws Exception {
        mockMvc.perform(patch("/api/v1/inventory/22222222-2222-2222-2222-222222222221")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111111")
                                        .claim("email", "admin@launchforge.dev")
                                        .claim("roles", java.util.List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAdjustmentPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("22222222-2222-2222-2222-222222222221")))
                .andExpect(jsonPath("$.availableQuantity", is(10)));
    }

    @Test
    void patchInventoryReturnsConflictForStaleVersion() throws Exception {
        mockMvc.perform(patch("/api/v1/inventory/22222222-2222-2222-2222-222222222221")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111111")
                                        .claim("email", "admin@launchforge.dev")
                                        .claim("roles", java.util.List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation":"INCREASE",
                                  "quantity":1,
                                  "version":999
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.type", containsString("inventory/optimistic-lock-conflict")));
    }

    private String validAdjustmentPayload() {
        return """
                {
                  "operation":"INCREASE",
                  "quantity":2,
                  "version":0
                }
                """;
    }
}

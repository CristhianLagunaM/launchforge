package com.launchforge.discounts;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import java.util.List;
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
class DiscountConfigurationControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void customerCannotModifyDiscountConfiguration() throws Exception {
        mockMvc.perform(patch("/api/v1/discount-configurations/TIME_RANGE")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111112")
                                        .claim("email", "customer@launchforge.dev")
                                        .claim("roles", List.of("CUSTOMER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void adminCanModifyDiscountConfiguration() throws Exception {
        mockMvc.perform(patch("/api/v1/discount-configurations/TIME_RANGE")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111111")
                                        .claim("email", "admin@launchforge.dev")
                                        .claim("roles", List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("TIME_RANGE")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.percentage", is(12.5)))
                .andExpect(jsonPath("$.updatedBy", is("11111111-1111-1111-1111-111111111111")));
    }

    @Test
    void adminCanListDiscountConfigurations() throws Exception {
        mockMvc.perform(get("/api/v1/discount-configurations")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111111")
                                        .claim("email", "admin@launchforge.dev")
                                        .claim("roles", List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists());
    }

    @Test
    void invalidConfigurationReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/discount-configurations/TIME_RANGE")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111111")
                                        .claim("email", "admin@launchforge.dev")
                                        .claim("roles", List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "percentage": 10.00,
                                  "startAt": "2026-08-31T23:59:59Z",
                                  "endAt": "2026-08-01T00:00:00Z",
                                  "minimumOrders": null,
                                  "lookbackMonths": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", containsString("discounts/invalid-configuration")));
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

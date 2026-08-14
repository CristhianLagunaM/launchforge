package com.launchforge.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ProductControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProductsIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].active", hasItem(true)));
    }

    @Test
    void createProductRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void createProductRejectsCustomerRole() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111112")
                                        .claim("email", "customer@launchforge.dev")
                                        .claim("roles", java.util.List.of("CUSTOMER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductPayload()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void createProductAllowsAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111111")
                                        .claim("email", "admin@launchforge.dev")
                                        .claim("roles", java.util.List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("LF-NEW-API-001"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getProductReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/v1/products/99999999-9999-9999-9999-999999999999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void createProductValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111111")
                                        .claim("email", "admin@launchforge.dev")
                                        .claim("roles", java.util.List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"",
                                  "name":"",
                                  "slug":"",
                                  "description":"",
                                  "categoryId":1,
                                  "price":-1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    private String validProductPayload() {
        return """
                {
                  "sku":"LF-NEW-API-001",
                  "name":"API Created Product",
                  "slug":"api-created-product",
                  "description":"Created through MockMvc",
                  "categoryId":1,
                  "price":1700.00
                }
                """;
    }
}

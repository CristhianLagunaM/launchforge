package com.launchforge.orders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import com.launchforge.persistence.model.inventory.Inventory;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetMutableFixtures() {
        jdbcTemplate.update(
                "DELETE FROM order_discounts WHERE order_id IN (SELECT id FROM orders WHERE idempotency_key LIKE 'mvc-idem-%')"
        );
        jdbcTemplate.update(
                "DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE idempotency_key LIKE 'mvc-idem-%')"
        );
        jdbcTemplate.update("DELETE FROM orders WHERE idempotency_key LIKE 'mvc-idem-%'");
        jdbcTemplate.update(
                "UPDATE inventory SET available_quantity = 8, reserved_quantity = 1 WHERE product_id = ?",
                UUID.fromString("22222222-2222-2222-2222-222222222221")
        );
    }

    @Test
    void createOrderRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void createOrderAllowsCustomerRole() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(customerJwt())
                        .header("Idempotency-Key", "mvc-idem-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.items[0].sku", is("LF-LANDING-001")));
    }

    @Test
    void customerCannotReadAnotherUsersOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/44444444-4444-4444-4444-444444444401")
                        .with(customerJwt()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)));
    }

    @Test
    void listOrdersReturnsOnlyAuthenticatedUsersOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .with(customerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].customerEmail", everyItem(is("customer@launchforge.dev"))));
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingOrder() throws Exception {
        var firstResponse = mockMvc.perform(post("/api/v1/orders")
                        .with(customerJwt())
                        .header("Idempotency-Key", "mvc-idem-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderPayload()))
                .andExpect(status().isCreated())
                .andReturn();

        String firstOrderId = com.jayway.jsonpath.JsonPath.read(firstResponse.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/orders")
                        .with(customerJwt())
                        .header("Idempotency-Key", "mvc-idem-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(firstOrderId)));
    }

    @Test
    void createOrderReturnsConflictWhenInventoryIsInsufficient() throws Exception {
        Inventory inventory = inventoryRepository.findByProduct_Id(UUID.fromString("22222222-2222-2222-2222-222222222221")).orElseThrow();
        inventory.setAvailableQuantity(0);
        inventoryRepository.saveAndFlush(inventory);

        mockMvc.perform(post("/api/v1/orders")
                        .with(customerJwt())
                        .header("Idempotency-Key", "mvc-idem-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderPayload()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", containsString("inventory/insufficient-capacity")));
    }

    @Test
    void customerCanCancelOwnConfirmedOrder() throws Exception {
        var creationResponse = mockMvc.perform(post("/api/v1/orders")
                        .with(customerJwt())
                        .header("Idempotency-Key", "mvc-idem-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderPayload()))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = com.jayway.jsonpath.JsonPath.read(creationResponse.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch("/api/v1/orders/{id}/cancel", orderId)
                        .with(customerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void getOrderReturnsDiscountBreakdownForHistoricalOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/44444444-4444-4444-4444-444444444406")
                .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("11111111-1111-1111-1111-111111111113")
                                        .claim("email", "frequent@launchforge.dev")
                                        .claim("roles", java.util.List.of("CUSTOMER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discounts.length()").value(3))
                .andExpect(jsonPath("$.discounts[0].code").value("TIME_RANGE"))
                .andExpect(jsonPath("$.discounts[1].code").value("RANDOM_ORDER"))
                .andExpect(jsonPath("$.discounts[2].code").value("FREQUENT_CUSTOMER"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("11111111-1111-1111-1111-111111111112")
                        .claim("email", "customer@launchforge.dev")
                        .claim("roles", java.util.List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private String validOrderPayload() {
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
}

package com.launchforge.orders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import com.launchforge.persistence.model.inventory.Inventory;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("null")
class OrderControllerMockMvcTest extends AbstractPostgresIntegrationTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString(
                    "91111111-1111-1111-1111-111111111111"
            );

    private static final UUID OTHER_CUSTOMER_ID =
            UUID.fromString(
                    "92222222-2222-2222-2222-222222222222"
            );

    private static final String CUSTOMER_EMAIL =
            "mvc.customer@launchforge.dev";

    private static final String OTHER_CUSTOMER_EMAIL =
            "mvc.other.customer@launchforge.dev";

    private static final String PASSWORD_HASH =
            "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void resetMutableFixtures() {
        jdbcTemplate.update(
                """
                DELETE FROM order_discounts
                WHERE order_id IN (
                    SELECT id
                    FROM orders
                    WHERE idempotency_key LIKE 'mvc-idem-%'
                )
                """
        );

        jdbcTemplate.update(
                """
                DELETE FROM order_items
                WHERE order_id IN (
                    SELECT id
                    FROM orders
                    WHERE idempotency_key LIKE 'mvc-idem-%'
                )
                """
        );

        jdbcTemplate.update(
                """
                DELETE FROM orders
                WHERE idempotency_key LIKE 'mvc-idem-%'
                """
        );

        ensureCustomerExists(
                CUSTOMER_ID,
                CUSTOMER_EMAIL,
                "MVC",
                "Customer"
        );

        ensureCustomerExists(
                OTHER_CUSTOMER_ID,
                OTHER_CUSTOMER_EMAIL,
                "MVC",
                "Other Customer"
        );

        UUID productId =
                activeProductId();

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET
                    available_quantity = 8,
                    reserved_quantity = 0
                WHERE product_id = ?
                """,
                productId
        );
    }

    @Test
    void createOrderRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validOrderPayload()
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header().string(
                                "Content-Type",
                                containsString(
                                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                                )
                        )
                );
    }

    @Test
    void createOrderAllowsCustomerRole() throws Exception {
        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(
                                        customerJwt()
                                )
                                .header(
                                        "Idempotency-Key",
                                        "mvc-idem-001"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validOrderPayload()
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.status",
                                is("CREATED")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.items[0].sku",
                                is(activeProductSku())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.requirementDescription",
                                is(
                                        "Necesito una solución digital para captar nuevos clientes."
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.projectObjective",
                                is(
                                        "Aumentar las solicitudes de cotización."
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.contactEmail",
                                is(CUSTOMER_EMAIL)
                        )
                );
    }

    @Test
    void customerCannotReadAnotherUsersOrder() throws Exception {
        var creationResponse =
                mockMvc.perform(
                                post("/api/v1/orders")
                                        .with(
                                                otherCustomerJwt()
                                        )
                                        .header(
                                                "Idempotency-Key",
                                                "mvc-idem-other-001"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                otherCustomerOrderPayload()
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        String orderId =
                com.jayway.jsonpath.JsonPath.read(
                        creationResponse
                                .getResponse()
                                .getContentAsString(),
                        "$.id"
                );

        mockMvc.perform(
                        get(
                                "/api/v1/orders/{id}",
                                orderId
                        )
                                .with(
                                        customerJwt()
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        header().string(
                                "Content-Type",
                                containsString(
                                        MediaType.APPLICATION_PROBLEM_JSON_VALUE
                                )
                        )
                );
    }

    @Test
    void listOrdersReturnsOnlyAuthenticatedUsersOrders() throws Exception {
        mockMvc.perform(
                        get("/api/v1/orders")
                                .with(
                                        customerJwt()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$[*].customerEmail",
                                everyItem(
                                        is(CUSTOMER_EMAIL)
                                )
                        )
                );
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingOrder() throws Exception {
        var firstResponse =
                mockMvc.perform(
                                post("/api/v1/orders")
                                        .with(
                                                customerJwt()
                                        )
                                        .header(
                                                "Idempotency-Key",
                                                "mvc-idem-002"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                validOrderPayload()
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        String firstOrderId =
                com.jayway.jsonpath.JsonPath.read(
                        firstResponse
                                .getResponse()
                                .getContentAsString(),
                        "$.id"
                );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(
                                        customerJwt()
                                )
                                .header(
                                        "Idempotency-Key",
                                        "mvc-idem-002"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validOrderPayload()
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.id",
                                is(firstOrderId)
                        )
                );
    }

    @Test
    void createOrderReturnsConflictWhenInventoryIsInsufficient()
            throws Exception {

        UUID productId =
                activeProductId();

        Inventory inventory =
                inventoryRepository
                        .findByProduct_Id(
                                productId
                        )
                        .orElseThrow();

        inventory.setAvailableQuantity(0);
        inventory.setReservedQuantity(0);

        inventoryRepository.saveAndFlush(
                inventory
        );

        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(
                                        customerJwt()
                                )
                                .header(
                                        "Idempotency-Key",
                                        "mvc-idem-003"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validOrderPayload()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath(
                                "$.type",
                                containsString(
                                        "inventory/insufficient-capacity"
                                )
                        )
                );
    }

    @Test
    void customerCanCancelOwnCreatedOrder() throws Exception {
        var creationResponse =
                mockMvc.perform(
                                post("/api/v1/orders")
                                        .with(
                                                customerJwt()
                                        )
                                        .header(
                                                "Idempotency-Key",
                                                "mvc-idem-004"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                validOrderPayload()
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.status",
                                        is("CREATED")
                                )
                        )
                        .andReturn();

        String orderId =
                com.jayway.jsonpath.JsonPath.read(
                        creationResponse
                                .getResponse()
                                .getContentAsString(),
                        "$.id"
                );

        mockMvc.perform(
                        patch(
                                "/api/v1/orders/{id}/cancel",
                                orderId
                        )
                                .with(
                                        customerJwt()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.status",
                                is("CANCELLED")
                        )
                );
    }

    @Test
    void getOrderReturnsCustomerRequirements() throws Exception {
        var creationResponse =
                mockMvc.perform(
                                post("/api/v1/orders")
                                        .with(
                                                customerJwt()
                                        )
                                        .header(
                                                "Idempotency-Key",
                                                "mvc-idem-005"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                validOrderPayload()
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        String orderId =
                com.jayway.jsonpath.JsonPath.read(
                        creationResponse
                                .getResponse()
                                .getContentAsString(),
                        "$.id"
                );

        mockMvc.perform(
                        get(
                                "/api/v1/orders/{id}",
                                orderId
                        )
                                .with(
                                        customerJwt()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.requirementDescription",
                                is(
                                        "Necesito una solución digital para captar nuevos clientes."
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.projectObjective",
                                is(
                                        "Aumentar las solicitudes de cotización."
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.contactEmail",
                                is(CUSTOMER_EMAIL)
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.contactPhone",
                                is("+57 300 000 0000")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.referencesUrl",
                                is(
                                        "https://example.com/reference"
                                )
                        )
                );
    }

    private void ensureCustomerExists(
            UUID id,
            String email,
            String firstName,
            String lastName
    ) {
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
                id,
                email,
                PASSWORD_HASH,
                firstName,
                lastName
        );
    }

    private UUID activeProductId() {
        UUID productId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT p.id
                        FROM products p
                        JOIN inventory i
                            ON i.product_id = p.id
                        WHERE p.active = TRUE
                        ORDER BY p.sku
                        LIMIT 1
                        """,
                        UUID.class
                );

        if (productId == null) {
            throw new IllegalStateException(
                    "No active product with inventory is available for order tests."
            );
        }

        return productId;
    }

    private String activeProductSku() {
        String sku =
                jdbcTemplate.queryForObject(
                        """
                        SELECT p.sku
                        FROM products p
                        JOIN inventory i
                            ON i.product_id = p.id
                        WHERE p.active = TRUE
                        ORDER BY p.sku
                        LIMIT 1
                        """,
                        String.class
                );

        if (sku == null) {
            throw new IllegalStateException(
                    "No active product SKU is available for order tests."
            );
        }

        return sku;
    }

    private org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors
            .JwtRequestPostProcessor customerJwt() {

        return jwt()
                .jwt(
                        jwt ->
                                jwt
                                        .subject(
                                                CUSTOMER_ID.toString()
                                        )
                                        .claim(
                                                "email",
                                                CUSTOMER_EMAIL
                                        )
                                        .claim(
                                                "roles",
                                                List.of(
                                                        "CUSTOMER"
                                                )
                                        )
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_CUSTOMER"
                        )
                );
    }

    private org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors
            .JwtRequestPostProcessor otherCustomerJwt() {

        return jwt()
                .jwt(
                        jwt ->
                                jwt
                                        .subject(
                                                OTHER_CUSTOMER_ID.toString()
                                        )
                                        .claim(
                                                "email",
                                                OTHER_CUSTOMER_EMAIL
                                        )
                                        .claim(
                                                "roles",
                                                List.of(
                                                        "CUSTOMER"
                                                )
                                        )
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_CUSTOMER"
                        )
                );
    }

    private String validOrderPayload() {
        return """
                {
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 1
                    }
                  ],
                  "requirementDescription": "Necesito una solución digital para captar nuevos clientes.",
                  "projectObjective": "Aumentar las solicitudes de cotización.",
                  "contactEmail": "%s",
                  "contactPhone": "+57 300 000 0000",
                  "referencesUrl": "https://example.com/reference"
                }
                """.formatted(
                activeProductId(),
                CUSTOMER_EMAIL
        );
    }

    private String otherCustomerOrderPayload() {
        return """
                {
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 1
                    }
                  ],
                  "requirementDescription": "Necesito una solución para un cliente diferente.",
                  "projectObjective": "Validar el control de acceso entre clientes.",
                  "contactEmail": "%s"
                }
                """.formatted(
                activeProductId(),
                OTHER_CUSTOMER_EMAIL
        );
    }
}

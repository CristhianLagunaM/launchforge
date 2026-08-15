package com.launchforge.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.launchforge.orders.api.dto.CreateOrderRequest;
import com.launchforge.orders.api.dto.OrderItemRequest;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.application.CreateOrderUseCase;
import com.launchforge.orders.application.IdempotentOrderQueryService;
import com.launchforge.orders.application.TransactionalOrderCreator;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderItem;
import com.launchforge.persistence.model.orders.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock
    private IdempotentOrderQueryService idempotentOrderQueryService;

    @Mock
    private TransactionalOrderCreator transactionalOrderCreator;

    @Test
    void returnsExistingOrderOnIdempotencyHit() {
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111112");
        CreateOrderUseCase createOrderUseCase = new CreateOrderUseCase(
                idempotentOrderQueryService,
                transactionalOrderCreator
        );

        CustomerOrder existingOrder = order(customerId);
        OrderResponse existingResponse = new com.launchforge.orders.application.OrderMapper().toResponse(existingOrder);
        when(idempotentOrderQueryService.findExisting(customerId, "idem-123"))
                .thenReturn(Optional.of(existingResponse));

        OrderResponse response = createOrderUseCase.createOrder(
                customerId,
                new CreateOrderRequest(List.of(new OrderItemRequest(UUID.randomUUID(), 1))),
                "idem-123"
        );

        assertThat(response.id()).isEqualTo(existingOrder.getId());
        assertThat(response.idempotencyKey()).isEqualTo("idem-123");
        verifyNoInteractions(transactionalOrderCreator);
    }

    private CustomerOrder order(UUID customerId) {
        User customer = new User();
        customer.setId(customerId);
        customer.setEmail("customer@launchforge.dev");
        customer.setFirstName("Camila");
        customer.setLastName("Customer");
        customer.setEnabled(true);

        Product product = new Product();
        product.setId(UUID.fromString("22222222-2222-2222-2222-222222222221"));
        product.setName("Landing Page Launch");
        product.setSku("LF-LANDING-001");

        OrderItem item = new OrderItem();
        item.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"));
        item.setProduct(product);
        item.setProductName("Landing Page Launch");
        item.setSku("LF-LANDING-001");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1200.00"));
        item.setSubtotal(new BigDecimal("1200.00"));

        CustomerOrder order = new CustomerOrder();
        order.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        order.setOrderNumber("LF-2026-TEST");
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSubtotal(new BigDecimal("1200.00"));
        order.setDiscountTotal(new BigDecimal("0.00"));
        order.setTotal(new BigDecimal("1200.00"));
        order.setIdempotencyKey("idem-123");
        order.setCreatedAt(Instant.parse("2026-08-14T18:00:00Z"));
        order.setUpdatedAt(Instant.parse("2026-08-14T18:00:00Z"));
        order.addItem(item);
        return order;
    }
}

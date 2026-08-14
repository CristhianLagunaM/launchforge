package com.launchforge.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.launchforge.inventory.application.InventoryManagementService;
import com.launchforge.orders.application.CancelOrderUseCase;
import com.launchforge.orders.application.OrderMapper;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderItem;
import com.launchforge.persistence.model.orders.OrderStatus;
import com.launchforge.shared.exception.ApiConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryManagementService inventoryManagementService;

    @Test
    void cancelsConfirmedOrderAndRestoresInventory() {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(orderRepository, new OrderMapper(), inventoryManagementService);
        CustomerOrder order = confirmedOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cancelOrderUseCase.cancelOrder(order.getId(), order.getCustomer().getId(), false);

        assertThat(response.status().name()).isEqualTo("CANCELLED");
        verify(inventoryManagementService).restoreCapacity(UUID.fromString("22222222-2222-2222-2222-222222222221"), 2);
    }

    @Test
    void rejectsDoubleCancellation() {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(orderRepository, new OrderMapper(), inventoryManagementService);
        CustomerOrder order = confirmedOrder();
        order.cancel();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> cancelOrderUseCase.cancelOrder(order.getId(), order.getCustomer().getId(), false))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void rejectsForeignOwnershipForCustomerRequests() {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(orderRepository, new OrderMapper(), inventoryManagementService);
        CustomerOrder order = confirmedOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> cancelOrderUseCase.cancelOrder(
                order.getId(),
                UUID.fromString("11111111-1111-1111-1111-111111111113"),
                false
        )).isInstanceOf(AccessDeniedException.class);
    }

    private CustomerOrder confirmedOrder() {
        User customer = new User();
        customer.setId(UUID.fromString("11111111-1111-1111-1111-111111111112"));
        customer.setEmail("customer@launchforge.dev");
        customer.setFirstName("Camila");
        customer.setLastName("Customer");
        customer.setEnabled(true);

        Product product = new Product();
        product.setId(UUID.fromString("22222222-2222-2222-2222-222222222221"));
        product.setSku("LF-LANDING-001");
        product.setName("Landing Page Launch");

        OrderItem item = new OrderItem();
        item.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"));
        item.setProduct(product);
        item.setProductName("Landing Page Launch");
        item.setSku("LF-LANDING-001");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("1200.00"));
        item.setSubtotal(new BigDecimal("2400.00"));

        CustomerOrder order = new CustomerOrder();
        order.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        order.setOrderNumber("LF-2026-TEST");
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSubtotal(new BigDecimal("2400.00"));
        order.setDiscountTotal(new BigDecimal("0.00"));
        order.setTotal(new BigDecimal("2400.00"));
        order.setCreatedAt(Instant.parse("2026-08-14T18:00:00Z"));
        order.setUpdatedAt(Instant.parse("2026-08-14T18:00:00Z"));
        order.addItem(item);
        return order;
    }
}

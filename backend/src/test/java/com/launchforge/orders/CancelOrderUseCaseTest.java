package com.launchforge.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CancelOrderUseCaseTest {

    private static final UUID CUSTOMER_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111112");

    private static final UUID FOREIGN_CUSTOMER_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111113");

    private static final UUID PRODUCT_ID = UUID.fromString(
            "22222222-2222-2222-2222-222222222221");

    private static final UUID ORDER_ID = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID ORDER_ITEM_ID = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryManagementService inventoryManagementService;

    @Test
    void cancelsCreatedOrderAndReleasesInventoryReservation() {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepository,
                new OrderMapper(),
                inventoryManagementService);

        CustomerOrder order = createdOrder();

        when(
                orderRepository.findById(
                        order.getId()))
                .thenReturn(
                        Optional.of(order));

        when(
                orderRepository.saveAndFlush(
                        any(CustomerOrder.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0));

        var response = cancelOrderUseCase.cancelOrder(
                order.getId(),
                order.getCustomer().getId(),
                false);

        assertThat(
                response.status()).isEqualTo(
                        OrderStatus.CANCELLED);

        verify(
                inventoryManagementService).releaseReservation(
                        PRODUCT_ID,
                        2);
    }

    @Test
    void rejectsDoubleCancellation() {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepository,
                new OrderMapper(),
                inventoryManagementService);

        CustomerOrder order = createdOrder();

        order.cancel();

        when(
                orderRepository.findById(
                        order.getId()))
                .thenReturn(
                        Optional.of(order));

        assertThatThrownBy(
                () -> cancelOrderUseCase.cancelOrder(
                        order.getId(),
                        order.getCustomer().getId(),
                        false))
                .isInstanceOf(
                        ApiConflictException.class)
                .hasMessageContaining(
                        "already cancelled");
    }

    @Test
    void rejectsForeignOwnershipForCustomerRequests() {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepository,
                new OrderMapper(),
                inventoryManagementService);

        CustomerOrder order = createdOrder();

        when(
                orderRepository.findById(
                        order.getId()))
                .thenReturn(
                        Optional.of(order));

        assertThatThrownBy(
                () -> cancelOrderUseCase.cancelOrder(
                        order.getId(),
                        FOREIGN_CUSTOMER_ID,
                        false))
                .isInstanceOf(
                        AccessDeniedException.class);
    }

    @Test
    void rejectsCancellationOfConfirmedOrder() {
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepository,
                new OrderMapper(),
                inventoryManagementService);

        CustomerOrder order = createdOrder();

        order.setStatus(
                OrderStatus.CONFIRMED);

        when(
                orderRepository.findById(
                        order.getId()))
                .thenReturn(
                        Optional.of(order));

        assertThatThrownBy(
                () -> cancelOrderUseCase.cancelOrder(
                        order.getId(),
                        order.getCustomer().getId(),
                        false))
                .isInstanceOf(
                        ApiConflictException.class)
                .hasMessageContaining(
                        "Solo se pueden cancelar órdenes pendientes de confirmación");
    }

    private CustomerOrder createdOrder() {
        User customer = new User();

        customer.setId(
                CUSTOMER_ID);

        customer.setEmail(
                "customer@launchforge.dev");

        customer.setFirstName(
                "Camila");

        customer.setLastName(
                "Customer");

        customer.setEnabled(
                true);

        Product product = new Product();

        product.setId(
                PRODUCT_ID);

        product.setSku(
                "LF-LANDING-001");

        product.setName(
                "Landing Page Launch");

        OrderItem item = new OrderItem();

        item.setId(
                ORDER_ITEM_ID);

        item.setProduct(
                product);

        item.setProductName(
                "Landing Page Launch");

        item.setSku(
                "LF-LANDING-001");

        item.setQuantity(
                2);

        item.setUnitPrice(
                new BigDecimal("1200.00"));

        item.setSubtotal(
                new BigDecimal("2400.00"));

        CustomerOrder order = new CustomerOrder();

        order.setId(
                ORDER_ID);

        order.setOrderNumber(
                "LF-2026-TEST");

        order.setCustomer(
                customer);

        order.setStatus(
                OrderStatus.CREATED);

        order.setSubtotal(
                new BigDecimal("2400.00"));

        order.setDiscountTotal(
                new BigDecimal("0.00"));

        order.setTotal(
                new BigDecimal("2400.00"));

        order.setRequirementDescription(
                "Necesito una landing page para captar clientes.");

        order.setProjectObjective(
                "Aumentar las solicitudes de cotización.");

        order.setContactEmail(
                "customer@launchforge.dev");

        order.setContactPhone(
                "+57 300 000 0000");

        order.setReferencesUrl(
                "https://example.com/reference");

        order.setCreatedAt(
                Instant.parse(
                        "2026-08-14T18:00:00Z"));

        order.setUpdatedAt(
                Instant.parse(
                        "2026-08-14T18:00:00Z"));

        order.addItem(
                item);

        return order;
    }
}

package com.launchforge.orders.application;

import com.launchforge.inventory.application.InventoryManagementService;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderItem;
import com.launchforge.persistence.model.orders.OrderStatus;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.launchforge.audit.application.AuditAction;
import com.launchforge.audit.application.LogAction;

@Service
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryManagementService inventoryManagementService;

    public CancelOrderUseCase(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            InventoryManagementService inventoryManagementService
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.inventoryManagementService = inventoryManagementService;
    }

    @Transactional
    @LogAction(action = AuditAction.ORDER_CANCELLED, resource = "ORDER", resourceId = "#result.id()")
    public OrderResponse cancelOrder(UUID orderId, UUID requesterId, boolean adminRequest) {
        CustomerOrder order = loadOrder(orderId);
        validateOwnership(order, requesterId, adminRequest);
        validateCancelableStatus(order);

        order.cancel();
        if (order.getStatus() == OrderStatus.CREATED) {
            for (OrderItem item : order.getItems()) {
                inventoryManagementService.releaseReservation(item.getProduct().getId(), item.getQuantity());
            }
        }

        return orderMapper.toResponse(orderRepository.saveAndFlush(order));
    }

    private CustomerOrder loadOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Order not found",
                        "Order not found for id: " + orderId,
                        "orders/not-found"
                ));
    }

    private void validateOwnership(CustomerOrder order, UUID requesterId, boolean adminRequest) {
        if (!adminRequest && !order.getCustomer().getId().equals(requesterId)) {
            throw new AccessDeniedException("You are not allowed to access this order.");
        }
    }

    private void validateCancelableStatus(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiConflictException(
                    "Invalid cancellation",
                    "Order is already cancelled.",
                    "orders/already-cancelled"
            );
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ApiConflictException(
                    "Invalid cancellation",
                    "Solo se pueden cancelar órdenes pendientes de confirmación. Las órdenes confirmadas o completadas son definitivas.",
                    "orders/invalid-cancel-status"
            );
        }
    }
}

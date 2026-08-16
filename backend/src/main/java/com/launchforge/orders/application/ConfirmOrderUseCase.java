package com.launchforge.orders.application;

import com.launchforge.inventory.application.InventoryManagementService;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderStatus;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.launchforge.audit.application.AuditAction;
import com.launchforge.audit.application.LogAction;

@Service
public class ConfirmOrderUseCase {
    private final OrderRepository orders;
    private final OrderMapper mapper;
    private final InventoryManagementService inventory;
    public ConfirmOrderUseCase(OrderRepository orders, OrderMapper mapper, InventoryManagementService inventory) { this.orders = orders; this.mapper = mapper; this.inventory = inventory; }
    @Transactional
    @LogAction(action = AuditAction.ORDER_CONFIRMED, resource = "ORDER", resourceId = "#result.id()")
    public OrderResponse confirm(UUID id) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new ApiNotFoundException("Orden no encontrada", "No existe la orden solicitada.", "orders/not-found"));
        if (order.getStatus() != OrderStatus.CREATED) throw new ApiConflictException("Orden no pendiente", "Solo se pueden confirmar órdenes pendientes.", "orders/invalid-confirm-status");
        order.getItems().forEach(item -> inventory.confirmReservation(item.getProduct().getId(), item.getQuantity()));
        order.setStatus(OrderStatus.CONFIRMED);
        return mapper.toResponse(orders.saveAndFlush(order));
    }
}

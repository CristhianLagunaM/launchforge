package com.launchforge.orders.application;

import com.launchforge.audit.application.AuditAction;
import com.launchforge.audit.application.LogAction;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderStatus;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompleteOrderUseCase {
    private final OrderRepository orders;
    private final OrderMapper mapper;
    public CompleteOrderUseCase(OrderRepository orders, OrderMapper mapper) { this.orders = orders; this.mapper = mapper; }
    @Transactional
    @LogAction(action = AuditAction.ORDER_COMPLETED, resource = "ORDER", resourceId = "#result.id()")
    public OrderResponse complete(UUID id) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new ApiNotFoundException("Orden no encontrada", "No existe la orden solicitada.", "orders/not-found"));
        if (order.getStatus() != OrderStatus.CONFIRMED) throw new ApiConflictException("Orden no confirmada", "Solo se pueden completar órdenes confirmadas.", "orders/invalid-complete-status");
        order.setStatus(OrderStatus.COMPLETED);
        return mapper.toResponse(orders.saveAndFlush(order));
    }
}

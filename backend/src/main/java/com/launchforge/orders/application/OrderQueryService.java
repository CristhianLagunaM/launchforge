package com.launchforge.orders.application;

import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderQueryService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(UUID requesterId) {
        List<CustomerOrder> orders = orderRepository.findAllByCustomer_IdOrderByCreatedAtDesc(requesterId);
        return orders.stream().map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID requesterId, boolean adminRequest) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Order not found",
                        "Order not found for id: " + orderId,
                        "orders/not-found"
                ));
        if (!adminRequest && !order.getCustomer().getId().equals(requesterId)) {
            throw new AccessDeniedException("You are not allowed to access this order.");
        }
        return orderMapper.toResponse(order);
    }
}

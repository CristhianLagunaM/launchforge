package com.launchforge.orders.application;

import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.infrastructure.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotentOrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public IdempotentOrderQueryService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findExisting(UUID customerId, String idempotencyKey) {
        return orderRepository.findByCustomer_IdAndIdempotencyKey(customerId, idempotencyKey)
                .map(orderMapper::toResponse);
    }
}

package com.launchforge.orders.application;

import com.launchforge.orders.api.dto.CreateOrderRequest;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.shared.exception.ApiConflictException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;

@Service
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final TransactionalOrderCreator transactionalOrderCreator;

    public CreateOrderUseCase(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            TransactionalOrderCreator transactionalOrderCreator
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.transactionalOrderCreator = transactionalOrderCreator;
    }

    public OrderResponse createOrder(UUID customerId, CreateOrderRequest request, String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            return orderRepository.findByCustomer_IdAndIdempotencyKey(customerId, normalizedIdempotencyKey)
                    .map(orderMapper::toResponse)
                    .orElseGet(() -> createNewOrder(customerId, request, normalizedIdempotencyKey));
        }

        return createNewOrder(customerId, request, null);
    }

    private OrderResponse createNewOrder(UUID customerId, CreateOrderRequest request, String idempotencyKey) {
        try {
            return transactionalOrderCreator.create(customerId, request, idempotencyKey);
        } catch (DataIntegrityViolationException | UnexpectedRollbackException exception) {
            if (idempotencyKey != null) {
                return orderRepository.findByCustomer_IdAndIdempotencyKey(customerId, idempotencyKey)
                        .map(orderMapper::toResponse)
                        .orElseThrow(() -> new ApiConflictException(
                                "Order conflict",
                                "A concurrent order request with the same idempotency key was detected.",
                                "orders/idempotency-conflict"
                        ));
            }
            throw exception;
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
    }
}

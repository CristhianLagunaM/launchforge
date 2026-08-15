package com.launchforge.orders.application;

import com.launchforge.orders.api.dto.CreateOrderRequest;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.shared.exception.ApiConflictException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;

@Service
public class CreateOrderUseCase {

    private final IdempotentOrderQueryService idempotentOrderQueryService;
    private final TransactionalOrderCreator transactionalOrderCreator;

    public CreateOrderUseCase(
            IdempotentOrderQueryService idempotentOrderQueryService,
            TransactionalOrderCreator transactionalOrderCreator
    ) {
        this.idempotentOrderQueryService = idempotentOrderQueryService;
        this.transactionalOrderCreator = transactionalOrderCreator;
    }

    public OrderResponse createOrder(UUID customerId, CreateOrderRequest request, String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            return idempotentOrderQueryService.findExisting(customerId, normalizedIdempotencyKey)
                    .orElseGet(() -> createNewOrder(customerId, request, normalizedIdempotencyKey));
        }

        return createNewOrder(customerId, request, null);
    }

    private OrderResponse createNewOrder(UUID customerId, CreateOrderRequest request, String idempotencyKey) {
        try {
            return transactionalOrderCreator.create(customerId, request, idempotencyKey);
        } catch (DataIntegrityViolationException | UnexpectedRollbackException exception) {
            if (idempotencyKey != null) {
                return idempotentOrderQueryService.findExisting(customerId, idempotencyKey)
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

package com.launchforge.orders.api.dto;

import com.launchforge.persistence.model.orders.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        String customerEmail,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items,
        List<OrderDiscountResponse> discounts
) {
}

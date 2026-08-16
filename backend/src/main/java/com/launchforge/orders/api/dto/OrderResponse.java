package com.launchforge.orders.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.launchforge.persistence.model.orders.OrderStatus;

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

        String requirementDescription,
        String projectObjective,
        String contactEmail,
        String contactPhone,
        LocalDate desiredDeliveryDate,
        String referencesUrl,

        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items,
        List<OrderDiscountResponse> discounts
) {
}

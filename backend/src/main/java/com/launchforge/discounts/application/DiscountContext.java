package com.launchforge.discounts.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountContext(
        UUID orderId,
        UUID customerId,
        Instant createdAt,
        BigDecimal subtotal,
        BigDecimal baseAmount
) {
}

package com.launchforge.orders.api.dto;

import java.math.BigDecimal;

public record OrderDiscountResponse(
        String code,
        BigDecimal percentage,
        BigDecimal baseAmount,
        BigDecimal amount,
        String reason,
        Integer applicationOrder
) {
}

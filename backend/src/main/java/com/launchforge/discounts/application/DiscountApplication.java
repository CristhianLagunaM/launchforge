package com.launchforge.discounts.application;

import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import java.math.BigDecimal;

public record DiscountApplication(
        DiscountConfiguration configuration,
        DiscountCode code,
        BigDecimal percentage,
        BigDecimal baseAmount,
        BigDecimal amount,
        String reason,
        int applicationOrder
) {
}

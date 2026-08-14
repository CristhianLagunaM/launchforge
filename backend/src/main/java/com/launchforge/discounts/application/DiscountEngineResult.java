package com.launchforge.discounts.application;

import java.math.BigDecimal;
import java.util.List;

public record DiscountEngineResult(
        BigDecimal discountTotal,
        BigDecimal finalTotal,
        List<DiscountApplication> appliedDiscounts
) {
}

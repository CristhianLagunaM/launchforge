package com.launchforge.discounts.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public record DiscountConfigurationUpdateRequest(
        @NotNull Boolean enabled,
        @NotNull @PositiveOrZero @Digits(integer = 3, fraction = 2) BigDecimal percentage,
        Instant startAt,
        Instant endAt,
        @Positive Integer minimumOrders,
        @Positive Integer lookbackMonths
) {
}

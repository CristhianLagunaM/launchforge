package com.launchforge.discounts.api.dto;

import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountConfigurationView(
        UUID id,
        String code,
        String type,
        Boolean enabled,
        BigDecimal percentage,
        Instant startAt,
        Instant endAt,
        Integer minimumOrders,
        Integer lookbackMonths,
        Instant createdAt,
        Instant updatedAt,
        UUID updatedBy
) {
    public static DiscountConfigurationView from(DiscountConfiguration configuration) {
        return new DiscountConfigurationView(
                configuration.getId(),
                configuration.getCode(),
                configuration.getType(),
                configuration.getEnabled(),
                configuration.getPercentage(),
                configuration.getStartAt(),
                configuration.getEndAt(),
                configuration.getMinimumOrders(),
                configuration.getLookbackMonths(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt(),
                configuration.getUpdatedBy()
        );
    }
}

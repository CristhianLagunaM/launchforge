package com.launchforge.discounts.application;

import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import com.launchforge.shared.exception.ApiConflictException;
import java.math.BigDecimal;

public final class DiscountConfigurationRules {

    private DiscountConfigurationRules() {
    }

    public static void validateForExecution(DiscountConfiguration configuration) {
        if (configuration.getCode() == null || configuration.getType() == null) {
            throw invalidConfiguration(configuration, "Discount configuration code/type must be present.");
        }
        DiscountCode code;
        try {
            code = DiscountCode.valueOf(configuration.getCode());
        } catch (IllegalArgumentException exception) {
            throw invalidConfiguration(configuration, "Discount configuration code is not supported by the engine.");
        }
        if (configuration.getPercentage() == null) {
            throw invalidConfiguration(configuration, "Discount percentage must be present.");
        }
        if (configuration.getPercentage().compareTo(BigDecimal.ZERO) < 0
                || configuration.getPercentage().compareTo(new BigDecimal("100.00")) > 0) {
            throw invalidConfiguration(configuration, "Discount percentage must stay between 0 and 100.");
        }
        if (configuration.getStartAt() != null
                && configuration.getEndAt() != null
                && configuration.getStartAt().isAfter(configuration.getEndAt())) {
            throw invalidConfiguration(configuration, "Discount time range is invalid because startAt is after endAt.");
        }

        if ((code == DiscountCode.TIME_RANGE || code == DiscountCode.RANDOM_ORDER)
                && (configuration.getStartAt() == null || configuration.getEndAt() == null)) {
            throw invalidConfiguration(configuration, "Time-bound discounts require both startAt and endAt.");
        }
        if (code == DiscountCode.FREQUENT_CUSTOMER
                && (configuration.getMinimumOrders() == null || configuration.getLookbackMonths() == null)) {
            throw invalidConfiguration(configuration, "Frequent customer discount requires minimumOrders and lookbackMonths.");
        }
    }

    public static ApiConflictException invalidConfiguration(DiscountConfiguration configuration, String detail) {
        return new ApiConflictException(
                "Invalid discount configuration",
                "%s [code=%s]".formatted(detail, configuration != null ? configuration.getCode() : "UNKNOWN"),
                "discounts/invalid-configuration"
        );
    }
}

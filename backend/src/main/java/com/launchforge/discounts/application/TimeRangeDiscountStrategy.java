package com.launchforge.discounts.application;

import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TimeRangeDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountCode code() {
        return DiscountCode.TIME_RANGE;
    }

    @Override
    public int applicationOrder() {
        return 1;
    }

    @Override
    public boolean isApplicable(DiscountContext context, DiscountConfiguration configuration) {
        return !context.createdAt().isBefore(configuration.getStartAt())
                && !context.createdAt().isAfter(configuration.getEndAt());
    }

    @Override
    public Optional<DiscountApplication> apply(DiscountContext context, DiscountConfiguration configuration) {
        BigDecimal baseAmount = context.baseAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = percentageAmount(baseAmount, configuration.getPercentage());
        return Optional.of(new DiscountApplication(
                configuration,
                code(),
                configuration.getPercentage(),
                baseAmount,
                amount,
                "Order created inside configured promotional time range.",
                applicationOrder()
        ));
    }

    private BigDecimal percentageAmount(BigDecimal baseAmount, BigDecimal percentage) {
        return baseAmount.multiply(percentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}

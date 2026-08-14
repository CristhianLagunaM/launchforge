package com.launchforge.discounts.application;

import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RandomOrderDiscountStrategy implements DiscountStrategy {

    private final RandomProvider randomProvider;

    public RandomOrderDiscountStrategy(RandomProvider randomProvider) {
        this.randomProvider = randomProvider;
    }

    @Override
    public DiscountCode code() {
        return DiscountCode.RANDOM_ORDER;
    }

    @Override
    public int applicationOrder() {
        return 2;
    }

    @Override
    public boolean isApplicable(DiscountContext context, DiscountConfiguration configuration) {
        boolean insideRange = !context.createdAt().isBefore(configuration.getStartAt())
                && !context.createdAt().isAfter(configuration.getEndAt());
        return insideRange && randomProvider.isWinningOrder(context.orderId(), context.customerId());
    }

    @Override
    public Optional<DiscountApplication> apply(DiscountContext context, DiscountConfiguration configuration) {
        BigDecimal baseAmount = context.baseAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = baseAmount.multiply(configuration.getPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return Optional.of(new DiscountApplication(
                configuration,
                code(),
                configuration.getPercentage(),
                baseAmount,
                amount,
                "Random-order winner selected by the injected random provider inside the configured time range.",
                applicationOrder()
        ));
    }
}

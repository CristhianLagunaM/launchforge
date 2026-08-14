package com.launchforge.discounts.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountEngine {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final DiscountConfigurationService discountConfigurationService;
    private final List<DiscountStrategy> discountStrategies;

    public DiscountEngine(
            DiscountConfigurationService discountConfigurationService,
            List<DiscountStrategy> discountStrategies
    ) {
        this.discountConfigurationService = discountConfigurationService;
        this.discountStrategies = discountStrategies.stream()
                .sorted(Comparator.comparingInt(DiscountStrategy::applicationOrder))
                .toList();
    }

    public DiscountEngineResult applyDiscounts(DiscountContext initialContext) {
        Map<DiscountCode, com.launchforge.persistence.model.discounts.DiscountConfiguration> configurations =
                discountConfigurationService.getEnabledConfigurationsByCode();
        List<DiscountApplication> appliedDiscounts = new ArrayList<>();
        BigDecimal subtotal = initialContext.subtotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountTotal = ZERO;

        for (DiscountStrategy strategy : discountStrategies) {
            var configuration = configurations.get(strategy.code());
            if (configuration == null) {
                continue;
            }

            DiscountContext currentContext = new DiscountContext(
                    initialContext.orderId(),
                    initialContext.customerId(),
                    initialContext.createdAt(),
                    subtotal,
                    subtotal
            );

            if (!strategy.isApplicable(currentContext, configuration)) {
                continue;
            }

            DiscountApplication application = strategy.apply(currentContext, configuration).orElse(null);
            if (application == null || application.amount().compareTo(ZERO) <= 0) {
                continue;
            }

            discountTotal = discountTotal.add(application.amount()).setScale(2, RoundingMode.HALF_UP);
            appliedDiscounts.add(application);
        }

        BigDecimal finalTotal = subtotal.subtract(discountTotal).setScale(2, RoundingMode.HALF_UP);
        if (finalTotal.compareTo(ZERO) < 0) {
            throw DiscountConfigurationRules.invalidConfiguration(
                    null,
                    "Applied discounts produced a negative order total."
            );
        }

        return new DiscountEngineResult(discountTotal, finalTotal, appliedDiscounts);
    }
}

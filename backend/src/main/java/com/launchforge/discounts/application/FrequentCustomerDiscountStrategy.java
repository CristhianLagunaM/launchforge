package com.launchforge.discounts.application;

import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import com.launchforge.persistence.model.orders.OrderStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FrequentCustomerDiscountStrategy implements DiscountStrategy {

    private static final List<OrderStatus> VALID_STATUSES = List.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED);

    private final OrderRepository orderRepository;

    public FrequentCustomerDiscountStrategy(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public DiscountCode code() {
        return DiscountCode.FREQUENT_CUSTOMER;
    }

    @Override
    public int applicationOrder() {
        return 3;
    }

    @Override
    public boolean isApplicable(DiscountContext context, DiscountConfiguration configuration) {
        ZonedDateTime lookbackStart = ZonedDateTime.ofInstant(context.createdAt(), ZoneOffset.UTC)
                .minusMonths(configuration.getLookbackMonths());
        long validOrders = orderRepository.countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(
                context.customerId(),
                VALID_STATUSES,
                lookbackStart.toInstant()
        );
        return validOrders >= configuration.getMinimumOrders();
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
                "Frequent customer threshold reached in previous confirmed/completed orders.",
                applicationOrder()
        ));
    }
}

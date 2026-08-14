package com.launchforge.discounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.launchforge.discounts.application.DiscountCode;
import com.launchforge.discounts.application.DiscountConfigurationRules;
import com.launchforge.discounts.application.DiscountConfigurationService;
import com.launchforge.discounts.application.DiscountContext;
import com.launchforge.discounts.application.DiscountEngine;
import com.launchforge.discounts.application.FrequentCustomerDiscountStrategy;
import com.launchforge.discounts.application.RandomOrderDiscountStrategy;
import com.launchforge.discounts.application.RandomProvider;
import com.launchforge.discounts.application.TimeRangeDiscountStrategy;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import com.launchforge.persistence.model.orders.OrderStatus;
import com.launchforge.shared.exception.ApiConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscountEngineTest {

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111113");
    private static final Instant INSIDE_RANGE = Instant.parse("2026-08-14T12:00:00Z");
    private static final Instant OUTSIDE_RANGE = Instant.parse("2026-09-14T12:00:00Z");

    @Mock
    private DiscountConfigurationService discountConfigurationService;

    @Mock
    private OrderRepository orderRepository;

    private DiscountEngine winningEngine;

    @BeforeEach
    void setUp() {
        winningEngine = new DiscountEngine(
                discountConfigurationService,
                List.of(
                        new TimeRangeDiscountStrategy(),
                        new RandomOrderDiscountStrategy(winningRandomProvider(true)),
                        new FrequentCustomerDiscountStrategy(orderRepository)
                )
        );
    }

    @Test
    void returnsOriginalSubtotalWhenNoDiscountConfigurationsAreEnabled() {
        when(discountConfigurationService.getEnabledConfigurationsByCode()).thenReturn(Map.of());

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("0.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("100.00");
        assertThat(result.appliedDiscounts()).isEmpty();
        verify(orderRepository, never()).countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(any(), any(), any());
    }

    @Test
    void appliesTimeRangeDiscountOnOriginalSubtotal() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(timeRange("10.00")));

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("10.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("90.00");
        assertThat(result.appliedDiscounts()).hasSize(1);
        assertThat(result.appliedDiscounts().getFirst().code()).isEqualTo(DiscountCode.TIME_RANGE);
        assertThat(result.appliedDiscounts().getFirst().baseAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void appliesRandomDiscountInsideConfiguredWindow() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(randomOrder("50.00")));

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("50.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("50.00");
        assertThat(result.appliedDiscounts()).extracting(application -> application.code().name())
                .containsExactly("RANDOM_ORDER");
    }

    @Test
    void doesNotApplyRandomDiscountOutsideConfiguredWindow() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(randomOrder("50.00")));

        var result = winningEngine.applyDiscounts(context(OUTSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("0.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("100.00");
        assertThat(result.appliedDiscounts()).isEmpty();
    }

    @Test
    void appliesFrequentCustomerDiscountWhenThresholdIsReached() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(frequentCustomer("5.00", 5, 12)));
        when(orderRepository.countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(
                eq(CUSTOMER_ID),
                eq(List.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED)),
                any(Instant.class)))
                .thenReturn(5L);

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("5.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("95.00");
        assertThat(result.appliedDiscounts()).hasSize(1);
        assertThat(result.appliedDiscounts().getFirst().code()).isEqualTo(DiscountCode.FREQUENT_CUSTOMER);
    }

    @Test
    void doesNotApplyFrequentCustomerDiscountWhenThresholdIsNotReached() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(frequentCustomer("5.00", 5, 12)));
        when(orderRepository.countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(
                eq(CUSTOMER_ID),
                eq(List.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED)),
                any(Instant.class)))
                .thenReturn(4L);

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("0.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("100.00");
        assertThat(result.appliedDiscounts()).isEmpty();
    }

    @Test
    void appliesTimeRangeAndFrequentDiscountsOnOriginalSubtotal() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(
                        timeRange("10.00"),
                        frequentCustomer("5.00", 5, 12)
                ));
        when(orderRepository.countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(
                eq(CUSTOMER_ID),
                eq(List.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED)),
                any(Instant.class)))
                .thenReturn(5L);

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("15.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("85.00");
        assertThat(result.appliedDiscounts()).extracting(application -> application.code().name())
                .containsExactly("TIME_RANGE", "FREQUENT_CUSTOMER");
        assertThat(result.appliedDiscounts().get(1).baseAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void appliesTimeRangeAndRandomDiscountsOnOriginalSubtotal() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(
                        timeRange("10.00"),
                        randomOrder("50.00")
                ));

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("60.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("40.00");
        assertThat(result.appliedDiscounts()).extracting(application -> application.code().name())
                .containsExactly("TIME_RANGE", "RANDOM_ORDER");
        assertThat(result.appliedDiscounts().get(1).baseAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void appliesAllDiscountsOnOriginalSubtotalWithExpectedApplicationOrder() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(
                        timeRange("10.00"),
                        randomOrder("50.00"),
                        frequentCustomer("5.00", 5, 12)
                ));
        when(orderRepository.countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(
                eq(CUSTOMER_ID),
                eq(List.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED)),
                any(Instant.class)))
                .thenReturn(7L);

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "100.00"));

        assertThat(result.discountTotal()).isEqualByComparingTo("65.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("35.00");
        assertThat(result.appliedDiscounts()).extracting(application -> application.code().name())
                .containsExactly("TIME_RANGE", "RANDOM_ORDER", "FREQUENT_CUSTOMER");
        assertThat(result.appliedDiscounts()).extracting(application -> application.applicationOrder())
                .containsExactly(1, 2, 3);
    }

    @Test
    void usesHalfUpRoundingForMonetaryAmounts() {
        when(discountConfigurationService.getEnabledConfigurationsByCode())
                .thenReturn(configurations(frequentCustomer("5.00", 1, 12)));
        when(orderRepository.countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(
                eq(CUSTOMER_ID),
                eq(List.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED)),
                any(Instant.class)))
                .thenReturn(1L);

        var result = winningEngine.applyDiscounts(context(INSIDE_RANGE, "99.99"));

        assertThat(result.discountTotal()).isEqualByComparingTo("5.00");
        assertThat(result.finalTotal()).isEqualByComparingTo("94.99");
    }

    @Test
    void rejectsInvalidConfiguredDateRange() {
        DiscountConfiguration configuration = timeRange("10.00");
        configuration.setStartAt(Instant.parse("2026-08-31T23:59:59Z"));
        configuration.setEndAt(Instant.parse("2026-08-01T00:00:00Z"));

        assertThatThrownBy(() -> DiscountConfigurationRules.validateForExecution(configuration))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("startAt is after endAt");
    }

    private DiscountContext context(Instant createdAt, String amount) {
        BigDecimal scaled = new BigDecimal(amount);
        return new DiscountContext(ORDER_ID, CUSTOMER_ID, createdAt, scaled, scaled);
    }

    private Map<DiscountCode, DiscountConfiguration> configurations(DiscountConfiguration... configurations) {
        Map<DiscountCode, DiscountConfiguration> result = new EnumMap<>(DiscountCode.class);
        for (DiscountConfiguration configuration : configurations) {
            result.put(DiscountCode.valueOf(configuration.getCode()), configuration);
        }
        return result;
    }

    private DiscountConfiguration timeRange(String percentage) {
        DiscountConfiguration configuration = baseConfiguration("TIME_RANGE", percentage);
        configuration.setStartAt(Instant.parse("2026-08-01T00:00:00Z"));
        configuration.setEndAt(Instant.parse("2026-08-31T23:59:59Z"));
        return configuration;
    }

    private DiscountConfiguration randomOrder(String percentage) {
        DiscountConfiguration configuration = baseConfiguration("RANDOM_ORDER", percentage);
        configuration.setStartAt(Instant.parse("2026-08-01T00:00:00Z"));
        configuration.setEndAt(Instant.parse("2026-08-31T23:59:59Z"));
        return configuration;
    }

    private DiscountConfiguration frequentCustomer(String percentage, int minimumOrders, int lookbackMonths) {
        DiscountConfiguration configuration = baseConfiguration("FREQUENT_CUSTOMER", percentage);
        configuration.setMinimumOrders(minimumOrders);
        configuration.setLookbackMonths(lookbackMonths);
        return configuration;
    }

    private DiscountConfiguration baseConfiguration(String code, String percentage) {
        DiscountConfiguration configuration = new DiscountConfiguration();
        configuration.setId(UUID.randomUUID());
        configuration.setCode(code);
        configuration.setType(code);
        configuration.setEnabled(true);
        configuration.setPercentage(new BigDecimal(percentage));
        configuration.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        configuration.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return configuration;
    }

    private RandomProvider winningRandomProvider(boolean winningOrder) {
        return (orderId, customerId) -> winningOrder;
    }
}

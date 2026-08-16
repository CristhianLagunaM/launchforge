package com.launchforge.report.infrastructure;

import java.math.BigDecimal;

public interface DashboardSummaryProjection {
    BigDecimal getGrossRevenue();

    BigDecimal getNetRevenue();

    BigDecimal getDiscountTotal();

    BigDecimal getAverageTicket();

    Long getTotalOrders();

    Long getPendingOrders();

    Long getConfirmedOrders();

    Long getCompletedOrders();

    Long getCancelledOrders();

    Long getAvailableCapacity();

    Long getReservedCapacity();

    Long getOutOfStockProducts();
}

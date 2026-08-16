package com.launchforge.report.infrastructure;

import java.math.BigDecimal;

public interface MonthlyRevenueProjection {
    String getPeriod();

    BigDecimal getRevenue();

    Long getOrderCount();
}

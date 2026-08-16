package com.launchforge.report.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardReport(
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        BigDecimal discountTotal,
        BigDecimal averageTicket,
        long totalOrders,
        OrderStatusReport ordersByStatus,
        CapacityReport capacity,
        List<MonthlyRevenueReport> monthlyRevenue,
        Instant generatedAt) {}

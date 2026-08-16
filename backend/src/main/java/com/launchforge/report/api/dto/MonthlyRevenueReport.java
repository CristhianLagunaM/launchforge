package com.launchforge.report.api.dto;

import java.math.BigDecimal;

public record MonthlyRevenueReport(String period, BigDecimal revenue, long orderCount) {}

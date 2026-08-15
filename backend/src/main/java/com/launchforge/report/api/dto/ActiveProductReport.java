package com.launchforge.report.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ActiveProductReport(
        UUID id,
        String sku,
        String name,
        String category,
        BigDecimal price
) {
}


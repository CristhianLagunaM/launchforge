package com.launchforge.report.api.dto;

import java.util.UUID;

public record TopProductReport(
        UUID productId,
        String sku,
        String name,
        long quantitySold
) {
}


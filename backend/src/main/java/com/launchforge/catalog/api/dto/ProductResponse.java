package com.launchforge.catalog.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String slug,
        String description,
        CategoryView category,
        BigDecimal price,
        boolean active,
        boolean available,
        Instant createdAt,
        Instant updatedAt
) {

    public record CategoryView(
            Long id,
            String name,
            String slug,
            boolean active
    ) {
    }
}

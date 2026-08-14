package com.launchforge.catalog.application;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String name,
        String sku,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean active,
        Boolean available
) {
}

package com.launchforge.inventory.api.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        String sku,
        String productName,
        boolean productActive,
        int availableQuantity,
        int reservedQuantity,
        long version,
        Instant updatedAt
) {
}

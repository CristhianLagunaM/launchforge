package com.launchforge.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryAdjustmentRequest(
        @NotNull InventoryAdjustmentOperation operation,
        @NotNull @Min(1) Integer quantity,
        @NotNull @Min(0) Long version
) {
}

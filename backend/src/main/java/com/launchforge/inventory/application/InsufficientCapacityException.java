package com.launchforge.inventory.application;

import com.launchforge.shared.exception.ApiConflictException;
import java.util.UUID;

public class InsufficientCapacityException extends ApiConflictException {

    private final UUID productId;
    private final String sku;
    private final String productName;
    private final int availableQuantity;
    private final int requestedQuantity;

    public InsufficientCapacityException(
            UUID productId,
            String sku,
            String productName,
            int availableQuantity,
            int requestedQuantity
    ) {
        super(
                "Insufficient inventory",
                "There is not enough available capacity for the requested product.",
                "inventory/insufficient-capacity"
        );
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}

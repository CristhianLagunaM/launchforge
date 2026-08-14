package com.launchforge.persistence.model.inventory;

import java.util.UUID;

public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(UUID productId, int availableQuantity, int requestedQuantity) {
        super("Insufficient inventory for product %s. Available: %d, requested: %d."
                .formatted(productId, availableQuantity, requestedQuantity));
    }
}

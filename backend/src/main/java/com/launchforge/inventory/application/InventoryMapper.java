package com.launchforge.inventory.application;

import com.launchforge.inventory.api.dto.InventoryResponse;
import com.launchforge.persistence.model.inventory.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getProduct().getId(),
                inventory.getProduct().getSku(),
                inventory.getProduct().getName(),
                Boolean.TRUE.equals(inventory.getProduct().getActive()),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getVersion(),
                inventory.getUpdatedAt()
        );
    }
}

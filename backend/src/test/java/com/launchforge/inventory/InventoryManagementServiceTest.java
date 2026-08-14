package com.launchforge.inventory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.inventory.api.dto.InventoryAdjustmentOperation;
import com.launchforge.inventory.api.dto.InventoryAdjustmentRequest;
import com.launchforge.inventory.application.InventoryManagementService;
import com.launchforge.inventory.application.InventoryMapper;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.inventory.Inventory;
import com.launchforge.shared.exception.ApiConflictException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryManagementServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Test
    void rejectsStaleVersionOnAdjustment() {
        InventoryManagementService inventoryManagementService =
                new InventoryManagementService(inventoryRepository, new InventoryMapper());
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222221");
        Inventory inventory = inventory(productId, 5, 3L);
        when(inventoryRepository.findByProduct_Id(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryManagementService.adjustInventory(
                productId,
                new InventoryAdjustmentRequest(InventoryAdjustmentOperation.INCREASE, 2, 2L)
        ))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Inventory version is stale");
    }

    private Inventory inventory(UUID productId, int availableQuantity, long version) {
        Product product = new Product();
        product.setId(productId);
        product.setSku("LF-LANDING-001");
        product.setName("Landing Page Launch");
        product.setActive(true);

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(availableQuantity);
        inventory.setReservedQuantity(0);
        inventory.setVersion(version);
        inventory.setUpdatedAt(Instant.parse("2026-08-14T12:00:00Z"));
        return inventory;
    }
}

package com.launchforge.inventory.application;

import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.inventory.api.dto.InventoryAdjustmentOperation;
import com.launchforge.inventory.api.dto.InventoryAdjustmentRequest;
import com.launchforge.inventory.api.dto.InventoryResponse;
import com.launchforge.persistence.model.inventory.InsufficientInventoryException;
import com.launchforge.persistence.model.inventory.Inventory;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryManagementService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryManagementService(InventoryRepository inventoryRepository, InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> listInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(inventoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID productId) {
        return inventoryMapper.toResponse(loadInventory(productId));
    }

    @Transactional
    public InventoryResponse adjustInventory(UUID productId, InventoryAdjustmentRequest request) {
        Inventory inventory = loadInventory(productId);
        validateVersion(inventory, request.version());
        applyOperation(inventory, request.operation(), request.quantity());
        try {
            Inventory savedInventory = inventoryRepository.saveAndFlush(inventory);
            return inventoryMapper.toResponse(savedInventory);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ApiConflictException(
                    "Inventory conflict",
                    "Inventory was updated by another request. Reload and retry.",
                    "inventory/optimistic-lock-conflict"
            );
        }
    }

    @Transactional
    public InventoryResponse consumeCapacity(UUID productId, int quantity) {
        Inventory inventory = loadInventory(productId);
        applyOperation(inventory, InventoryAdjustmentOperation.DECREASE, quantity);
        try {
            Inventory savedInventory = inventoryRepository.saveAndFlush(inventory);
            return inventoryMapper.toResponse(savedInventory);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ApiConflictException(
                    "Inventory conflict",
                    "Inventory was updated by another request. Reload and retry.",
                    "inventory/optimistic-lock-conflict"
            );
        }
    }

    private void validateVersion(Inventory inventory, long expectedVersion) {
        if (inventory.getVersion() != expectedVersion) {
            throw new ApiConflictException(
                    "Inventory conflict",
                    "Inventory version is stale. Reload and retry with the latest state.",
                    "inventory/optimistic-lock-conflict"
            );
        }
    }

    private void applyOperation(Inventory inventory, InventoryAdjustmentOperation operation, int quantity) {
        try {
            switch (operation) {
                case INCREASE -> inventory.increase(quantity);
                case DECREASE -> inventory.decrease(quantity);
                case RESTORE -> inventory.restore(quantity);
            }
        } catch (InsufficientInventoryException exception) {
            throw new ApiConflictException(
                    "Insufficient inventory",
                    exception.getMessage(),
                    "inventory/insufficient-capacity"
            );
        }
    }

    private Inventory loadInventory(UUID productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Inventory not found",
                        "Inventory not found for product id: " + productId,
                        "inventory/not-found"
                ));
    }
}

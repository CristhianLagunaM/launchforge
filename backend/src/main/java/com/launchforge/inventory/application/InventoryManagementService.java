package com.launchforge.inventory.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.launchforge.audit.application.AuditAction;
import com.launchforge.audit.application.LogAction;
import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.inventory.api.dto.InventoryAdjustmentOperation;
import com.launchforge.inventory.api.dto.InventoryAdjustmentRequest;
import com.launchforge.inventory.api.dto.InventoryResponse;
import com.launchforge.persistence.model.inventory.InsufficientInventoryException;
import com.launchforge.persistence.model.inventory.Inventory;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;

@Service
public class InventoryManagementService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryManagementService(
            InventoryRepository inventoryRepository,
            InventoryMapper inventoryMapper
    ) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> listInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable)
                .map(inventoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID productId) {
        return inventoryMapper.toResponse(
                loadInventory(productId)
        );
    }

    @Transactional
    @LogAction(
            action = AuditAction.INVENTORY_ADJUSTED,
            resource = "INVENTORY",
            resourceId = "#result.productId()"
    )
    public InventoryResponse adjustInventory(
            UUID productId,
            InventoryAdjustmentRequest request
    ) {
        Inventory inventory = loadInventory(productId);

        validateVersion(
                inventory,
                request.version()
        );

        applyOperation(
                inventory,
                request.operation(),
                request.quantity()
        );

        try {
            Inventory inventoryToSave = Objects.requireNonNull(
                    inventory,
                    "Inventory to save must not be null"
            );

            Inventory savedInventory =
                    inventoryRepository.saveAndFlush(inventoryToSave);

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
    public InventoryResponse consumeCapacity(
            UUID productId,
            int quantity
    ) {
        return applyDirectAdjustment(
                productId,
                InventoryAdjustmentOperation.DECREASE,
                quantity
        );
    }

    @Transactional
    public InventoryResponse restoreCapacity(
            UUID productId,
            int quantity
    ) {
        return applyDirectAdjustment(
                productId,
                InventoryAdjustmentOperation.RESTORE,
                quantity
        );
    }

    @Transactional
    public InventoryResponse reserveCapacity(
            UUID productId,
            int quantity
    ) {
        return applyReservation(
                productId,
                quantity,
                1
        );
    }

    @Transactional
    public InventoryResponse confirmReservation(
            UUID productId,
            int quantity
    ) {
        return applyReservation(
                productId,
                quantity,
                2
        );
    }

    @Transactional
    public InventoryResponse releaseReservation(
            UUID productId,
            int quantity
    ) {
        return applyReservation(
                productId,
                quantity,
                3
        );
    }

    private InventoryResponse applyReservation(
            UUID productId,
            int quantity,
            int operation
    ) {
        Inventory inventory = loadInventory(productId);

        try {
            switch (operation) {
                case 1 -> inventory.reserve(quantity);
                case 2 -> inventory.confirmReservation(quantity);
                case 3 -> inventory.releaseReservation(quantity);
                default -> throw new IllegalArgumentException(
                        "Unsupported inventory reservation operation: "
                                + operation
                );
            }

            Inventory inventoryToSave = Objects.requireNonNull(
                    inventory,
                    "Inventory to save must not be null"
            );

            Inventory savedInventory =
                    inventoryRepository.saveAndFlush(inventoryToSave);

            return inventoryMapper.toResponse(savedInventory);

        } catch (InsufficientInventoryException exception) {
            throw new InsufficientCapacityException(
                    inventory.getProduct().getId(),
                    inventory.getProduct().getSku(),
                    inventory.getProduct().getName(),
                    inventory.getAvailableQuantity(),
                    quantity
            );
        }
    }

    private InventoryResponse applyDirectAdjustment(
            UUID productId,
            InventoryAdjustmentOperation operation,
            int quantity
    ) {
        Inventory inventory = loadInventory(productId);

        applyOperation(
                inventory,
                operation,
                quantity
        );

        try {
            Inventory inventoryToSave = Objects.requireNonNull(
                    inventory,
                    "Inventory to save must not be null"
            );

            Inventory savedInventory =
                    inventoryRepository.saveAndFlush(inventoryToSave);

            return inventoryMapper.toResponse(savedInventory);

        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ApiConflictException(
                    "Inventory conflict",
                    "Inventory was updated by another request. Reload and retry.",
                    "inventory/optimistic-lock-conflict"
            );
        }
    }

    private void validateVersion(
            Inventory inventory,
            long expectedVersion
    ) {
        if (inventory.getVersion() != expectedVersion) {
            throw new ApiConflictException(
                    "Inventory conflict",
                    "Inventory version is stale. Reload and retry with the latest state.",
                    "inventory/optimistic-lock-conflict"
            );
        }
    }

    private void applyOperation(
            Inventory inventory,
            InventoryAdjustmentOperation operation,
            int quantity
    ) {
        try {
            switch (operation) {
                case INCREASE -> inventory.increase(quantity);
                case DECREASE -> inventory.decrease(quantity);
                case RESTORE -> inventory.restore(quantity);
            }

        } catch (InsufficientInventoryException exception) {
            throw new InsufficientCapacityException(
                    inventory.getProduct().getId(),
                    inventory.getProduct().getSku(),
                    inventory.getProduct().getName(),
                    inventory.getAvailableQuantity(),
                    quantity
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

package com.launchforge.inventory.api;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.launchforge.inventory.api.dto.InventoryAdjustmentRequest;
import com.launchforge.inventory.api.dto.InventoryResponse;
import com.launchforge.inventory.application.InventoryManagementService;
import com.launchforge.shared.exception.ApiBadRequestException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryManagementService inventoryManagementService;

    public InventoryController(
            InventoryManagementService inventoryManagementService
    ) {
        this.inventoryManagementService = inventoryManagementService;
    }

    @GetMapping
    public Page<InventoryResponse> listInventory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(required = false) String sort
    ) {
        return inventoryManagementService.listInventory(
                toPageable(page, size, sort)
        );
    }

    @GetMapping("/{productId}")
    public InventoryResponse getInventory(
            @PathVariable UUID productId
    ) {
        return inventoryManagementService.getInventory(productId);
    }

    @PatchMapping("/{productId}")
    public InventoryResponse adjustInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryAdjustmentRequest request
    ) {
        return inventoryManagementService.adjustInventory(
                productId,
                request
        );
    }

    private Pageable toPageable(
            int page,
            int size,
            String sortParam
    ) {
        if (sortParam == null || sortParam.isBlank()) {
            return PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Order.asc("product.name"))
            );
        }

        String[] tokens = sortParam.split(",", 2);

        String property = Objects.requireNonNull(
                mapSortProperty(tokens[0]),
                "Sort property must not be null"
        );

        Sort.Direction direction = Sort.Direction.ASC;

        if (tokens.length == 2) {
            String directionValue = Objects.requireNonNull(
                    tokens[1]
                            .trim()
                            .toUpperCase(Locale.ROOT),
                    "Sort direction must not be null"
            );

            direction = Sort.Direction
                    .fromOptionalString(directionValue)
                    .orElseThrow(() -> new ApiBadRequestException(
                            "Invalid sort direction",
                            "Invalid sort direction for: " + sortParam,
                            "inventory/invalid-sort"
                    ));
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(new Sort.Order(direction, property))
        );
    }

    private String mapSortProperty(String property) {
        String normalizedProperty = property.trim();

        return switch (normalizedProperty) {
            case "productName" -> "product.name";
            case "sku" -> "product.sku";

            case "availableQuantity",
                    "reservedQuantity",
                    "version",
                    "updatedAt" -> normalizedProperty;

            default -> throw new ApiBadRequestException(
                    "Invalid sort field",
                    "Unsupported sort field: " + property,
                    "inventory/invalid-sort"
            );
        };
    }
}

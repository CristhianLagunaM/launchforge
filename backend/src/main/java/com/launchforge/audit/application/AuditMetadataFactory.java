package com.launchforge.audit.application;

import com.launchforge.catalog.api.dto.ProductResponse;
import com.launchforge.discounts.api.dto.DiscountConfigurationView;
import com.launchforge.inventory.api.dto.InventoryAdjustmentRequest;
import com.launchforge.inventory.api.dto.InventoryResponse;
import com.launchforge.orders.api.dto.OrderResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuditMetadataFactory {

    public Map<String, Object> create(AuditAction action, Object[] arguments, Object result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        switch (action) {
            case USER_CREATED -> metadata.put("assignedRole", "CUSTOMER");
            case PRODUCT_CREATED, PRODUCT_UPDATED -> addProduct(metadata, result);
            case PRODUCT_DISABLED -> {
                metadata.put("previousStatus", true);
                metadata.put("newStatus", false);
            }
            case INVENTORY_ADJUSTED -> addInventory(metadata, arguments, result);
            case ORDER_CREATED -> addOrder(metadata, result, false);
            case ORDER_CANCELLED -> addOrder(metadata, result, true);
            case DISCOUNT_CONFIGURATION_UPDATED -> addDiscount(metadata, result);
            case USER_STATUS_CHANGED, USER_ROLE_CHANGED -> { }
        }
        return Map.copyOf(metadata);
    }

    private void addProduct(Map<String, Object> metadata, Object result) {
        if (result instanceof ProductResponse product) {
            metadata.put("sku", product.sku());
            metadata.put("active", product.active());
        }
    }

    private void addInventory(Map<String, Object> metadata, Object[] arguments, Object result) {
        if (arguments.length > 1 && arguments[1] instanceof InventoryAdjustmentRequest request
                && result instanceof InventoryResponse inventory) {
            int previous = switch (request.operation()) {
                case INCREASE, RESTORE -> inventory.availableQuantity() - request.quantity();
                case DECREASE -> inventory.availableQuantity() + request.quantity();
            };
            metadata.put("operation", request.operation().name());
            metadata.put("previousQuantity", previous);
            metadata.put("newQuantity", inventory.availableQuantity());
        }
    }

    private void addOrder(Map<String, Object> metadata, Object result, boolean cancelled) {
        if (result instanceof OrderResponse order) {
            if (cancelled) {
                metadata.put("previousStatus", "CONFIRMED");
                metadata.put("newStatus", order.status().name());
            } else {
                metadata.put("status", order.status().name());
                metadata.put("itemCount", order.items().size());
                metadata.put("total", order.total());
            }
        }
    }

    private void addDiscount(Map<String, Object> metadata, Object result) {
        if (result instanceof DiscountConfigurationView configuration) {
            metadata.put("code", configuration.code());
            metadata.put("enabled", configuration.enabled());
            metadata.put("percentage", configuration.percentage());
        }
    }
}

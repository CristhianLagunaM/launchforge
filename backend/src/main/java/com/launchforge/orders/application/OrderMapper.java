package com.launchforge.orders.application;

import org.springframework.stereotype.Component;

import com.launchforge.orders.api.dto.OrderDiscountResponse;
import com.launchforge.orders.api.dto.OrderItemResponse;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.persistence.model.discounts.OrderDiscount;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderItem;

@Component
public class OrderMapper {

    public OrderResponse toResponse(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getId(),
                order.getCustomer().getEmail(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getTotal(),
                order.getIdempotencyKey(),

                order.getRequirementDescription(),
                order.getProjectObjective(),
                order.getContactEmail(),
                order.getContactPhone(),
                order.getDesiredDeliveryDate(),
                order.getReferencesUrl(),

                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList(),
                order.getOrderDiscounts()
                        .stream()
                        .map(this::toDiscountResponse)
                        .toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProductName(),
                item.getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    private OrderDiscountResponse toDiscountResponse(
            OrderDiscount discount
    ) {
        return new OrderDiscountResponse(
                discount.getCode(),
                discount.getPercentage(),
                discount.getBaseAmount(),
                discount.getAmount(),
                discount.getReason(),
                discount.getApplicationOrder()
        );
    }
}

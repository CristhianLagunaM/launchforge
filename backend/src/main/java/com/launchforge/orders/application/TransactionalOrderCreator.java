package com.launchforge.orders.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.launchforge.audit.application.AuditAction;
import com.launchforge.audit.application.LogAction;
import com.launchforge.auth.infrastructure.UserRepository;
import com.launchforge.catalog.infrastructure.ProductRepository;
import com.launchforge.discounts.application.DiscountApplication;
import com.launchforge.discounts.application.DiscountContext;
import com.launchforge.discounts.application.DiscountEngine;
import com.launchforge.discounts.application.DiscountEngineResult;
import com.launchforge.inventory.application.InventoryManagementService;
import com.launchforge.orders.api.dto.CreateOrderRequest;
import com.launchforge.orders.api.dto.OrderItemRequest;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.discounts.OrderDiscount;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderItem;
import com.launchforge.persistence.model.orders.OrderStatus;
import com.launchforge.shared.exception.ApiBadRequestException;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;

@Service
public class TransactionalOrderCreator {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryManagementService inventoryManagementService;
    private final DiscountEngine discountEngine;

    public TransactionalOrderCreator(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            InventoryManagementService inventoryManagementService,
            DiscountEngine discountEngine
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.inventoryManagementService = inventoryManagementService;
        this.discountEngine = discountEngine;
    }

    @Transactional
    @LogAction(
            action = AuditAction.ORDER_CREATED,
            resource = "ORDER",
            resourceId = "#result.id()"
    )
    public OrderResponse create(
            UUID customerId,
            CreateOrderRequest request,
            String idempotencyKey
    ) {
        User customer = loadCustomer(customerId);

        Map<UUID, Integer> consolidatedItems =
                consolidateItems(request);

        CustomerOrder order = new CustomerOrder();

        order.setId(UUID.randomUUID());
        order.setCustomer(customer);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.CREATED);
        order.setIdempotencyKey(idempotencyKey);

        order.setRequirementDescription(
                request.requirementDescription().trim()
        );

        order.setProjectObjective(
                request.projectObjective().trim()
        );

        order.setContactEmail(
                request.contactEmail().trim()
        );

        order.setContactPhone(
                normalizeOptionalText(request.contactPhone())
        );

        order.setDesiredDeliveryDate(
                request.desiredDeliveryDate()
        );

        order.setReferencesUrl(
                normalizeOptionalText(request.referencesUrl())
        );

        order.setDiscountTotal(ZERO);

        Instant orderCreatedAt = Instant.now();

        order.setCreatedAt(orderCreatedAt);
        order.setUpdatedAt(orderCreatedAt);

        BigDecimal subtotal = ZERO;

        for (Map.Entry<UUID, Integer> entry :
                consolidatedItems.entrySet()) {

            Product product =
                    loadProduct(entry.getKey());

            validateActiveProduct(product);

            inventoryManagementService.reserveCapacity(
                    product.getId(),
                    entry.getValue()
            );

            OrderItem item =
                    buildOrderItem(
                            product,
                            entry.getValue()
                    );

            subtotal =
                    subtotal.add(item.getSubtotal());

            order.addItem(item);
        }

        order.setSubtotal(
                subtotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                )
        );

        DiscountEngineResult discountResult =
                discountEngine.applyDiscounts(
                        new DiscountContext(
                                order.getId(),
                                customerId,
                                orderCreatedAt,
                                order.getSubtotal(),
                                order.getSubtotal()
                        )
                );

        applyDiscounts(
                order,
                discountResult
        );

        CustomerOrder savedOrder =
                orderRepository.saveAndFlush(order);

        return orderMapper.toResponse(savedOrder);
    }

    private Map<UUID, Integer> consolidateItems(
            CreateOrderRequest request
    ) {
        if (
                request.items() == null ||
                request.items().isEmpty()
        ) {
            throw new ApiBadRequestException(
                    "Invalid order items",
                    "Order must contain at least one item.",
                    "orders/invalid-items"
            );
        }

        Map<UUID, Integer> consolidated =
                new LinkedHashMap<>();

        for (OrderItemRequest item :
                request.items()) {

            if (item.quantity() <= 0) {
                throw new ApiBadRequestException(
                        "Invalid order items",
                        "Order item quantity must be greater than zero.",
                        "orders/invalid-items"
                );
            }

            consolidated.merge(
                    item.productId(),
                    item.quantity(),
                    Integer::sum
            );
        }

        return consolidated;
    }

    private User loadCustomer(UUID customerId) {
        return userRepository.findById(customerId)
                .filter(
                        user ->
                                Boolean.TRUE.equals(
                                        user.getEnabled()
                                )
                )
                .orElseThrow(
                        () ->
                                new ApiNotFoundException(
                                        "Customer not found",
                                        "Customer not found for id: "
                                                + customerId,
                                        "orders/customer-not-found"
                                )
                );
    }

    private Product loadProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(
                        () ->
                                new ApiNotFoundException(
                                        "Product not found",
                                        "Product not found for id: "
                                                + productId,
                                        "orders/product-not-found"
                                )
                );
    }

    private void validateActiveProduct(
            Product product
    ) {
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ApiConflictException(
                    "Inactive product",
                    "Product is inactive and cannot be ordered: "
                            + product.getSku(),
                    "orders/product-inactive"
            );
        }
    }

    private OrderItem buildOrderItem(
            Product product,
            int quantity
    ) {
        BigDecimal unitPrice =
                product.getPrice()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        OrderItem item = new OrderItem();

        item.setProduct(product);
        item.setProductName(product.getName());
        item.setSku(product.getSku());
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);

        item.setSubtotal(
                unitPrice
                        .multiply(
                                BigDecimal.valueOf(quantity)
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
        );

        return item;
    }

    private String generateOrderNumber() {
        LocalDate today =
                LocalDate.now(ZoneOffset.UTC);

        return "LF-%s-%s".formatted(
                today.getYear(),
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );
    }

    private void applyDiscounts(
            CustomerOrder order,
            DiscountEngineResult discountResult
    ) {
        for (
                DiscountApplication application :
                        discountResult.appliedDiscounts()
        ) {
            OrderDiscount orderDiscount =
                    new OrderDiscount();

            orderDiscount.setDiscountConfiguration(
                    application.configuration()
            );

            orderDiscount.setCode(
                    application.code().name()
            );

            orderDiscount.setPercentage(
                    application.percentage()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
            );

            orderDiscount.setBaseAmount(
                    application.baseAmount()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
            );

            orderDiscount.setAmount(
                    application.amount()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
            );

            orderDiscount.setReason(
                    application.reason()
            );

            orderDiscount.setApplicationOrder(
                    application.applicationOrder()
            );

            order.addDiscount(orderDiscount);
        }

        order.applyDiscountSummary(
                discountResult.discountTotal()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        ),
                discountResult.finalTotal()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
        );
    }

    private String normalizeOptionalText(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
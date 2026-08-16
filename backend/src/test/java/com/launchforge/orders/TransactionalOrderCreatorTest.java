package com.launchforge.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.launchforge.auth.infrastructure.UserRepository;
import com.launchforge.catalog.infrastructure.ProductRepository;
import com.launchforge.discounts.application.DiscountApplication;
import com.launchforge.discounts.application.DiscountCode;
import com.launchforge.discounts.application.DiscountEngine;
import com.launchforge.discounts.application.DiscountEngineResult;
import com.launchforge.inventory.application.InventoryManagementService;
import com.launchforge.orders.api.dto.CreateOrderRequest;
import com.launchforge.orders.api.dto.OrderItemRequest;
import com.launchforge.orders.api.dto.OrderResponse;
import com.launchforge.orders.application.OrderMapper;
import com.launchforge.orders.application.TransactionalOrderCreator;
import com.launchforge.orders.infrastructure.OrderRepository;
import com.launchforge.persistence.model.catalog.Category;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.discounts.DiscountConfiguration;
import com.launchforge.persistence.model.identity.User;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;

@ExtendWith(MockitoExtension.class)
class TransactionalOrderCreatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryManagementService inventoryManagementService;

    @Mock
    private DiscountEngine discountEngine;

    private TransactionalOrderCreator transactionalOrderCreator;

    @BeforeEach
    void setUp() {
        transactionalOrderCreator = new TransactionalOrderCreator(
                userRepository,
                productRepository,
                orderRepository,
                new OrderMapper(),
                inventoryManagementService,
                discountEngine
        );
    }

    @Test
    void createsOrderWithSubtotalAndSnapshot() {
        User customer = customer();

        Product product = product(
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222221"
                ),
                "LF-LANDING-001",
                "Landing Page Launch",
                true,
                "1200.00"
        );

        when(
                userRepository.findById(customer.getId())
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                productRepository.findById(product.getId())
        ).thenReturn(
                Optional.of(product)
        );

        when(
                discountEngine.applyDiscounts(any())
        ).thenReturn(
                new DiscountEngineResult(
                        new BigDecimal("0.00"),
                        new BigDecimal("2400.00"),
                        List.of()
                )
        );

        when(
                orderRepository.saveAndFlush(
                        any(CustomerOrder.class)
                )
        ).thenAnswer(invocation -> {
            CustomerOrder order =
                    invocation.getArgument(0);

            order.setId(
                    UUID.fromString(
                            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                    )
            );

            order.setCreatedAt(
                    Instant.parse(
                            "2026-08-14T18:00:00Z"
                    )
            );

            order.setUpdatedAt(
                    Instant.parse(
                            "2026-08-14T18:00:00Z"
                    )
            );

            return order;
        });

        OrderResponse response =
                transactionalOrderCreator.create(
                        customer.getId(),
                        request(
                                List.of(
                                        new OrderItemRequest(
                                                product.getId(),
                                                2
                                        )
                                )
                        ),
                        "idem-001"
                );

        assertThat(
                response.status().name()
        ).isEqualTo(
                "CREATED"
        );

        assertThat(
                response.subtotal()
        ).isEqualByComparingTo(
                "2400.00"
        );

        assertThat(
                response.total()
        ).isEqualByComparingTo(
                "2400.00"
        );

        assertThat(
                response.items()
        ).hasSize(
                1
        );

        assertThat(
                response.items()
                        .getFirst()
                        .productName()
        ).isEqualTo(
                "Landing Page Launch"
        );

        assertThat(
                response.items()
                        .getFirst()
                        .sku()
        ).isEqualTo(
                "LF-LANDING-001"
        );

        assertThat(
                response.items()
                        .getFirst()
                        .subtotal()
        ).isEqualByComparingTo(
                "2400.00"
        );

        assertThat(
                response.requirementDescription()
        ).isEqualTo(
                "Necesito una landing page para captar clientes."
        );

        assertThat(
                response.projectObjective()
        ).isEqualTo(
                "Aumentar las solicitudes de cotización."
        );

        assertThat(
                response.contactEmail()
        ).isEqualTo(
                "customer@launchforge.dev"
        );

        verify(
                inventoryManagementService
        ).reserveCapacity(
                product.getId(),
                2
        );
    }

    @Test
    void consolidatesRepeatedProductsBeforeReservingInventory() {
        User customer = customer();

        Product product = product(
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222221"
                ),
                "LF-LANDING-001",
                "Landing Page Launch",
                true,
                "1200.00"
        );

        when(
                userRepository.findById(customer.getId())
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                productRepository.findById(product.getId())
        ).thenReturn(
                Optional.of(product)
        );

        when(
                discountEngine.applyDiscounts(any())
        ).thenReturn(
                new DiscountEngineResult(
                        new BigDecimal("0.00"),
                        new BigDecimal("3600.00"),
                        List.of()
                )
        );

        when(
                orderRepository.saveAndFlush(
                        any(CustomerOrder.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        transactionalOrderCreator.create(
                customer.getId(),
                request(
                        List.of(
                                new OrderItemRequest(
                                        product.getId(),
                                        1
                                ),
                                new OrderItemRequest(
                                        product.getId(),
                                        2
                                )
                        )
                ),
                "idem-002"
        );

        verify(
                inventoryManagementService,
                times(1)
        ).reserveCapacity(
                product.getId(),
                3
        );

        ArgumentCaptor<CustomerOrder> captor =
                ArgumentCaptor.forClass(
                        CustomerOrder.class
                );

        verify(
                orderRepository
        ).saveAndFlush(
                captor.capture()
        );

        assertThat(
                captor.getValue()
                        .getItems()
        ).hasSize(
                1
        );

        assertThat(
                captor.getValue()
                        .getItems()
                        .getFirst()
                        .getQuantity()
        ).isEqualTo(
                3
        );
    }

    @Test
    void rejectsInactiveProducts() {
        User customer = customer();

        Product product = product(
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222221"
                ),
                "LF-LANDING-001",
                "Landing Page Launch",
                false,
                "1200.00"
        );

        when(
                userRepository.findById(customer.getId())
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                productRepository.findById(product.getId())
        ).thenReturn(
                Optional.of(product)
        );

        assertThatThrownBy(
                () ->
                        transactionalOrderCreator.create(
                                customer.getId(),
                                request(
                                        List.of(
                                                new OrderItemRequest(
                                                        product.getId(),
                                                        1
                                                )
                                        )
                                ),
                                "idem-003"
                        )
        )
                .isInstanceOf(
                        ApiConflictException.class
                )
                .hasMessageContaining(
                        "inactive"
                );
    }

    @Test
    void rejectsUnknownProducts() {
        User customer = customer();

        UUID productId =
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222221"
                );

        when(
                userRepository.findById(customer.getId())
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                productRepository.findById(productId)
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(
                () ->
                        transactionalOrderCreator.create(
                                customer.getId(),
                                request(
                                        List.of(
                                                new OrderItemRequest(
                                                        productId,
                                                        1
                                                )
                                        )
                                ),
                                "idem-004"
                        )
        )
                .isInstanceOf(
                        ApiNotFoundException.class
                );
    }

    @Test
    void appliesDiscountBreakdownToOrderTotals() {
        User customer = customer();

        Product product = product(
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222221"
                ),
                "LF-LANDING-001",
                "Landing Page Launch",
                true,
                "1200.00"
        );

        DiscountConfiguration configuration =
                new DiscountConfiguration();

        configuration.setId(
                UUID.fromString(
                        "55555555-5555-5555-5555-555555555551"
                )
        );

        configuration.setCode(
                "TIME_RANGE"
        );

        configuration.setType(
                "TIME_RANGE"
        );

        configuration.setEnabled(
                true
        );

        configuration.setPercentage(
                new BigDecimal("10.00")
        );

        when(
                userRepository.findById(customer.getId())
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                productRepository.findById(product.getId())
        ).thenReturn(
                Optional.of(product)
        );

        when(
                discountEngine.applyDiscounts(any())
        ).thenReturn(
                new DiscountEngineResult(
                        new BigDecimal("120.00"),
                        new BigDecimal("1080.00"),
                        List.of(
                                new DiscountApplication(
                                        configuration,
                                        DiscountCode.TIME_RANGE,
                                        new BigDecimal("10.00"),
                                        new BigDecimal("1200.00"),
                                        new BigDecimal("120.00"),
                                        "Order created inside configured promotional time range.",
                                        1
                                )
                        )
                )
        );

        when(
                orderRepository.saveAndFlush(
                        any(CustomerOrder.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        OrderResponse response =
                transactionalOrderCreator.create(
                        customer.getId(),
                        request(
                                List.of(
                                        new OrderItemRequest(
                                                product.getId(),
                                                1
                                        )
                                )
                        ),
                        "idem-005"
                );

        assertThat(
                response.discountTotal()
        ).isEqualByComparingTo(
                "120.00"
        );

        assertThat(
                response.total()
        ).isEqualByComparingTo(
                "1080.00"
        );

        assertThat(
                response.discounts()
        ).hasSize(
                1
        );

        assertThat(
                response.discounts()
                        .getFirst()
                        .code()
        ).isEqualTo(
                "TIME_RANGE"
        );

        assertThat(
                response.discounts()
                        .getFirst()
                        .amount()
        ).isEqualByComparingTo(
                "120.00"
        );
    }

    private CreateOrderRequest request(
            List<OrderItemRequest> items
    ) {
        return new CreateOrderRequest(
                items,
                "Necesito una landing page para captar clientes.",
                "Aumentar las solicitudes de cotización.",
                "customer@launchforge.dev",
                "+57 300 000 0000",
                null,
                "https://example.com/reference"
        );
    }

    private User customer() {
        User user = new User();

        user.setId(
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111112"
                )
        );

        user.setEmail(
                "customer@launchforge.dev"
        );

        user.setFirstName(
                "Camila"
        );

        user.setLastName(
                "Customer"
        );

        user.setEnabled(
                true
        );

        return user;
    }

    private Product product(
            UUID id,
            String sku,
            String name,
            boolean active,
            String price
    ) {
        Category category =
                new Category();

        category.setId(
                1L
        );

        category.setName(
                "WEB"
        );

        Product product =
                new Product();

        product.setId(
                id
        );

        product.setSku(
                sku
        );

        product.setName(
                name
        );

        product.setSlug(
                name.toLowerCase()
                        .replace(
                                ' ',
                                '-'
                        )
        );

        product.setDescription(
                "Snapshot"
        );

        product.setCategory(
                category
        );

        product.setActive(
                active
        );

        product.setPrice(
                new BigDecimal(price)
        );

        return product;
    }
}

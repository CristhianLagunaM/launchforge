package com.launchforge.report;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import com.launchforge.report.application.ReportQueryService;

@SpringBootTest
@Transactional
class ReportRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportQueryService reportQueryService;

    private long categoryId;

    @BeforeEach
    public void resetData() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE audit_log, order_discounts, order_items, orders, inventory,
                               products, categories, user_roles, users RESTART IDENTITY CASCADE
                """);

        jdbcTemplate.update("""
                INSERT INTO categories (
                    name,
                    slug,
                    description,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (
                    'REPORTS',
                    'reports',
                    'Controlled report test data',
                    TRUE,
                    ?,
                    ?
                )
                """,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );

        Long resolvedCategoryId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM categories
                        WHERE slug = 'reports'
                        """,
                        Long.class
                );

        if (resolvedCategoryId == null) {
            throw new IllegalStateException(
                    "Report test category could not be resolved."
            );
        }

        categoryId = resolvedCategoryId;
    }

    @Test
    void activeProductsExcludeInactiveAndUseStableOrdering() {
        insertProduct(
                "SKU-Z",
                "Zulu",
                true
        );

        insertProduct(
                "SKU-A",
                "Alpha",
                true
        );

        insertProduct(
                "SKU-X",
                "Hidden",
                false
        );

        var result =
                reportQueryService.activeProducts();

        assertThat(
                result
        )
                .extracting(
                        row -> row.name()
                )
                .containsExactly(
                        "Alpha",
                        "Zulu"
                );

        assertThat(
                result
        ).allMatch(
                row ->
                        row.category()
                                .equals("REPORTS")
        );
    }

    @Test
    void topProductsAggregateInPostgresExcludeCancelledLimitFiveAndBreakTiesByName() {
        UUID customerId =
                insertUser(
                        "products@reports.test",
                        "Product",
                        "Buyer"
                );

        List<UUID> productIds =
                List.of(
                        insertProduct(
                                "SKU-1",
                                "Atlas",
                                true
                        ),
                        insertProduct(
                                "SKU-2",
                                "Boreal",
                                true
                        ),
                        insertProduct(
                                "SKU-3",
                                "Cosmos",
                                true
                        ),
                        insertProduct(
                                "SKU-4",
                                "Delta",
                                true
                        ),
                        insertProduct(
                                "SKU-5",
                                "Echo",
                                true
                        ),
                        insertProduct(
                                "SKU-6",
                                "Foxtrot",
                                true
                        ),
                        insertProduct(
                                "SKU-7",
                                "Gamma",
                                true
                        )
                );

        int[] quantities = {
                12,
                9,
                8,
                7,
                6,
                5,
                5
        };

        for (
                int index = 0;
                index < productIds.size();
                index++
        ) {
            UUID orderId =
                    insertOrder(
                            customerId,
                            "CONFIRMED"
                    );

            insertOrderItem(
                    orderId,
                    productIds.get(index),
                    "SKU-" + (index + 1),
                    quantities[index]
            );
        }

        UUID cancelledOrder =
                insertOrder(
                        customerId,
                        "CANCELLED"
                );

        insertOrderItem(
                cancelledOrder,
                productIds.get(6),
                "SKU-7",
                100
        );

        var result =
                reportQueryService.topProducts();

        assertThat(
                result
        ).hasSize(
                5
        );

        assertThat(
                result
        )
                .extracting(
                        row -> row.name()
                )
                .containsExactly(
                        "Atlas",
                        "Boreal",
                        "Cosmos",
                        "Delta",
                        "Echo"
                );

        assertThat(
                result
        )
                .extracting(
                        row -> row.quantitySold()
                )
                .containsExactly(
                        12L,
                        9L,
                        8L,
                        7L,
                        6L
                );
    }

    @Test
    void topCustomersCountValidOrdersExcludeCancelledLimitFiveAndBreakTiesByEmail() {
        UUID productId =
                insertProduct(
                        "SKU-C",
                        "Customer report product",
                        true
                );

        String[] emails = {
                "zeta@test.dev",
                "alpha@test.dev",
                "bravo@test.dev",
                "charlie@test.dev",
                "delta@test.dev",
                "echo@test.dev",
                "foxtrot@test.dev"
        };

        int[] validCounts = {
                7,
                7,
                5,
                4,
                3,
                2,
                1
        };

        UUID lastCustomer = null;

        for (
                int customerIndex = 0;
                customerIndex < emails.length;
                customerIndex++
        ) {
            UUID customerId =
                    insertUser(
                            emails[customerIndex],
                            "Customer",
                            String.valueOf(customerIndex)
                    );

            lastCustomer =
                    customerId;

            for (
                    int orderIndex = 0;
                    orderIndex < validCounts[customerIndex];
                    orderIndex++
            ) {
                UUID orderId =
                        insertOrder(
                                customerId,
                                orderIndex % 2 == 0
                                        ? "CONFIRMED"
                                        : "COMPLETED"
                        );

                insertOrderItem(
                        orderId,
                        productId,
                        "SKU-C",
                        1
                );
            }
        }

        if (lastCustomer == null) {
            throw new IllegalStateException(
                    "No report test customer was created."
            );
        }

        UUID cancelledOrder =
                insertOrder(
                        lastCustomer,
                        "CANCELLED"
                );

        insertOrderItem(
                cancelledOrder,
                productId,
                "SKU-C",
                1
        );

        var result =
                reportQueryService.topCustomers();

        assertThat(
                result
        ).hasSize(
                5
        );

        assertThat(
                result
        )
                .extracting(
                        row -> row.email()
                )
                .containsExactly(
                        "alpha@test.dev",
                        "zeta@test.dev",
                        "bravo@test.dev",
                        "charlie@test.dev",
                        "delta@test.dev"
                );

        assertThat(
                result
        )
                .extracting(
                        row -> row.orderCount()
                )
                .containsExactly(
                        7L,
                        7L,
                        5L,
                        4L,
                        3L
                );
    }

    @Test
    void dashboardAggregatesMoneyStatusesCapacityAndMonthlyRevenueInPostgres() {
        UUID customerId =
                insertUser(
                        "dashboard@reports.test",
                        "Dashboard",
                        "Buyer"
                );

        UUID productId =
                insertProduct(
                        "SKU-D",
                        "Dashboard product",
                        true
                );

        jdbcTemplate.update("""
                INSERT INTO inventory (
                    id,
                    product_id,
                    available_quantity,
                    reserved_quantity,
                    version,
                    updated_at
                )
                VALUES (?, ?, 0, 2, 0, ?)
                """,
                UUID.randomUUID(),
                productId,
                Timestamp.from(Instant.now())
        );

        UUID confirmedOrder =
                insertOrder(
                        customerId,
                        "CONFIRMED"
                );

        jdbcTemplate.update("""
                UPDATE orders
                SET
                    subtotal = 200.00,
                    discount_total = 50.00,
                    total = 150.00
                WHERE id = ?
                """,
                confirmedOrder
        );

        insertOrder(
                customerId,
                "CREATED"
        );

        insertOrder(
                customerId,
                "CANCELLED"
        );

        var result =
                reportQueryService.dashboard();

        assertThat(
                result.grossRevenue()
        ).isEqualByComparingTo(
                "200.00"
        );

        assertThat(
                result.netRevenue()
        ).isEqualByComparingTo(
                "150.00"
        );

        assertThat(
                result.discountTotal()
        ).isEqualByComparingTo(
                "50.00"
        );

        assertThat(
                result.averageTicket()
        ).isEqualByComparingTo(
                "150.00"
        );

        assertThat(
                result.totalOrders()
        ).isEqualTo(
                3
        );

        assertThat(
                result.ordersByStatus().pending()
        ).isEqualTo(
                1
        );

        assertThat(
                result.ordersByStatus().confirmed()
        ).isEqualTo(
                1
        );

        assertThat(
                result.ordersByStatus().cancelled()
        ).isEqualTo(
                1
        );

        assertThat(
                result.capacity().available()
        ).isZero();

        assertThat(
                result.capacity().reserved()
        ).isEqualTo(
                2
        );

        assertThat(
                result.capacity().outOfStockProducts()
        ).isEqualTo(
                1
        );

        assertThat(
                result.monthlyRevenue()
        ).hasSize(
                6
        );

        assertThat(
                result.monthlyRevenue()
                        .get(5)
                        .revenue()
        ).isEqualByComparingTo(
                "150.00"
        );
    }

    private UUID insertProduct(
            String sku,
            String name,
            boolean active
    ) {
        UUID id =
                UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO products (
                    id,
                    sku,
                    name,
                    slug,
                    description,
                    category_id,
                    price,
                    active,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    'Report test product',
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                id,
                sku,
                name,
                name.toLowerCase()
                        .replace(
                                ' ',
                                '-'
                        ),
                categoryId,
                new BigDecimal("100.00"),
                active,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );

        return id;
    }

    private UUID insertUser(
            String email,
            String firstName,
            String lastName
    ) {
        UUID id =
                UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO users (
                    id,
                    email,
                    password_hash,
                    first_name,
                    last_name,
                    enabled,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    '$2b$10$test',
                    ?,
                    ?,
                    TRUE,
                    ?,
                    ?
                )
                """,
                id,
                email,
                firstName,
                lastName,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );

        return id;
    }

    private UUID insertOrder(
            UUID customerId,
            String status
    ) {
        UUID id =
                UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO orders (
                    id,
                    order_number,
                    customer_id,
                    status,
                    subtotal,
                    discount_total,
                    total,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    100.00,
                    0.00,
                    100.00,
                    ?,
                    ?
                )
                """,
                id,
                "TEST-" + id.toString().substring(0, 32),
                customerId,
                status,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );

        return id;
    }

    private void insertOrderItem(
            UUID orderId,
            UUID productId,
            String sku,
            int quantity
    ) {
        jdbcTemplate.update("""
                INSERT INTO order_items (
                    id,
                    order_id,
                    product_id,
                    product_name,
                    sku,
                    quantity,
                    unit_price,
                    subtotal
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    'Snapshot',
                    ?,
                    ?,
                    100.00,
                    ?
                )
                """,
                UUID.randomUUID(),
                orderId,
                productId,
                sku,
                quantity,
                new BigDecimal(quantity * 100)
        );
    }
}

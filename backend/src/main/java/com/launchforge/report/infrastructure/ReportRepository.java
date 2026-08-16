package com.launchforge.report.infrastructure;

import com.launchforge.persistence.model.catalog.Product;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface ReportRepository extends Repository<Product, UUID> {

    @Query(value = """
            SELECT p.id AS id,
                   p.sku AS sku,
                   p.name AS name,
                   c.name AS category,
                   p.price AS price
            FROM products p
            JOIN categories c ON c.id = p.category_id
            WHERE p.active = TRUE
            ORDER BY p.name ASC, p.sku ASC
            """, nativeQuery = true)
    List<ActiveProductProjection> findActiveProducts();

    @Query(value = """
            SELECT p.id AS productId,
                   p.sku AS sku,
                   p.name AS name,
                   SUM(oi.quantity) AS quantitySold
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN products p ON p.id = oi.product_id
            WHERE o.status IN ('CONFIRMED', 'COMPLETED')
            GROUP BY p.id, p.sku, p.name
            ORDER BY quantitySold DESC, p.name ASC, p.sku ASC
            LIMIT 5
            """, nativeQuery = true)
    List<TopProductProjection> findTopProducts();

    @Query(value = """
            SELECT u.id AS customerId,
                   u.email AS email,
                   u.first_name AS firstName,
                   u.last_name AS lastName,
                   COUNT(o.id) AS orderCount
            FROM orders o
            JOIN users u ON u.id = o.customer_id
            WHERE o.status IN ('CONFIRMED', 'COMPLETED')
            GROUP BY u.id, u.email, u.first_name, u.last_name
            ORDER BY orderCount DESC, u.email ASC
            LIMIT 5
            """, nativeQuery = true)
    List<TopCustomerProjection> findTopCustomers();

    @Query(value = """
            SELECT COALESCE(SUM(o.subtotal) FILTER (WHERE o.status IN ('CONFIRMED', 'COMPLETED')), 0) AS grossRevenue,
                   COALESCE(SUM(o.total) FILTER (WHERE o.status IN ('CONFIRMED', 'COMPLETED')), 0) AS netRevenue,
                   COALESCE(SUM(o.discount_total) FILTER (WHERE o.status IN ('CONFIRMED', 'COMPLETED')), 0) AS discountTotal,
                   COALESCE(AVG(o.total) FILTER (WHERE o.status IN ('CONFIRMED', 'COMPLETED')), 0) AS averageTicket,
                   COUNT(o.id) AS totalOrders,
                   COUNT(o.id) FILTER (WHERE o.status = 'CREATED') AS pendingOrders,
                   COUNT(o.id) FILTER (WHERE o.status = 'CONFIRMED') AS confirmedOrders,
                   COUNT(o.id) FILTER (WHERE o.status = 'COMPLETED') AS completedOrders,
                   COUNT(o.id) FILTER (WHERE o.status = 'CANCELLED') AS cancelledOrders,
                   (SELECT COALESCE(SUM(i.available_quantity), 0) FROM inventory i) AS availableCapacity,
                   (SELECT COALESCE(SUM(i.reserved_quantity), 0) FROM inventory i) AS reservedCapacity,
                   (SELECT COUNT(*) FROM inventory i JOIN products p ON p.id = i.product_id
                    WHERE p.active = TRUE AND i.available_quantity = 0) AS outOfStockProducts
            FROM orders o
            """, nativeQuery = true)
    DashboardSummaryProjection dashboardSummary();

    @Query(value = """
            WITH months AS (
                SELECT generate_series(
                    date_trunc('month', CURRENT_TIMESTAMP) - INTERVAL '5 months',
                    date_trunc('month', CURRENT_TIMESTAMP),
                    INTERVAL '1 month'
                ) AS month_start
            )
            SELECT TO_CHAR(m.month_start, 'YYYY-MM') AS period,
                   COALESCE(SUM(o.total), 0) AS revenue,
                   COUNT(o.id) AS orderCount
            FROM months m
            LEFT JOIN orders o ON o.created_at >= m.month_start
                              AND o.created_at < m.month_start + INTERVAL '1 month'
                              AND o.status IN ('CONFIRMED', 'COMPLETED')
            GROUP BY m.month_start
            ORDER BY m.month_start
            """, nativeQuery = true)
    List<MonthlyRevenueProjection> monthlyRevenue();
}

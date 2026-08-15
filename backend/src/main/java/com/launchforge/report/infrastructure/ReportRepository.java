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
}


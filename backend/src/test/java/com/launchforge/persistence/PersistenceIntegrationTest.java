package com.launchforge.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import com.launchforge.persistence.model.inventory.Inventory;
import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderItem;
import com.launchforge.persistence.model.orders.OrderStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID FREQUENT_ORDER_ID = UUID.fromString("44444444-4444-4444-4444-444444444406");
    private static final UUID INVENTORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @org.junit.jupiter.api.Test
    void flywayExecutesAllMigrationsAndHibernateValidatesSchema() {
        Integer installedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
                Integer.class);
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
                String.class);

        assertThat(installedCount).isEqualTo(10);
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
    }

    @org.junit.jupiter.api.Test
    void databaseConstraintsRejectInvalidData() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO users (
                    id, email, password_hash, first_name, last_name, enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, now(), now())
                """,
                UUID.randomUUID(),
                "admin@launchforge.dev",
                "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm",
                "Dup",
                "User",
                true))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO products (
                    id, sku, name, slug, description, category_id, price, active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """,
                UUID.randomUUID(),
                "LF-INVALID-001",
                "Invalid Product",
                "invalid-product",
                "Invalid seed",
                1L,
                "-1.00",
                true))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO inventory (
                    id, product_id, available_quantity, reserved_quantity, version, updated_at
                ) VALUES (?, ?, ?, ?, ?, now())
                """,
                UUID.randomUUID(),
                UUID.fromString("22222222-2222-2222-2222-222222222221"),
                -1,
                0,
                0L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @org.junit.jupiter.api.Test
    @Transactional
    void jpaRelationshipsExposeSeededAggregateGraph() {
        CustomerOrder order = entityManager.find(CustomerOrder.class, FREQUENT_ORDER_ID);

        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getCustomer().getEmail()).isEqualTo("frequent@launchforge.dev");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getOrderDiscounts()).hasSize(3);

        OrderItem firstItem = order.getItems().getFirst();
        assertThat(firstItem.getProductName()).isEqualTo("MVP SaaS Forge");
        assertThat(firstItem.getProduct().getCategory().getName()).isEqualTo("SAAS");
    }

    @org.junit.jupiter.api.Test
    @Transactional
    void inventoryUsesVersionForOptimisticLocking() {
        Inventory inventory = entityManager.find(Inventory.class, INVENTORY_ID);
        Long initialVersion = inventory.getVersion();

        inventory.setReservedQuantity(inventory.getReservedQuantity() + 1);
        entityManager.flush();
        entityManager.clear();

        Inventory reloaded = entityManager.find(Inventory.class, INVENTORY_ID);
        assertThat(reloaded.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(reloaded.getReservedQuantity()).isEqualTo(2);
    }

    @org.junit.jupiter.api.Test
    void discountSeedSupportsFrequentCustomerRule() {
        Map<String, Object> discount = jdbcTemplate.queryForMap(
                """
                SELECT code, minimum_orders, lookback_months
                FROM discount_configuration
                WHERE code = 'FREQUENT_CUSTOMER'
                """);

        assertThat(discount).containsEntry("code", "FREQUENT_CUSTOMER");
        assertThat(discount).containsEntry("minimum_orders", 5);
        assertThat(discount).containsEntry("lookback_months", 12);
    }

    @org.junit.jupiter.api.Test
    void randomDiscountMigrationConfiguresAConcreteDateRange() {
        Map<String, Object> discount = jdbcTemplate.queryForMap(
                """
                SELECT code, start_at, end_at
                FROM discount_configuration
                WHERE code = 'RANDOM_ORDER'
                """);

        assertThat(discount).containsEntry("code", "RANDOM_ORDER");
        assertThat(discount.get("start_at")).isNotNull();
        assertThat(discount.get("end_at")).isNotNull();
    }
}

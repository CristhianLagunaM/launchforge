package com.launchforge.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.inventory.Inventory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void flywayExecutesAllMigrationsAndHibernateValidatesSchema() {
        Integer installedCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE success = TRUE
                        """,
                        Integer.class
                );

        List<String> versions =
                jdbcTemplate.queryForList(
                        """
                        SELECT version
                        FROM flyway_schema_history
                        WHERE version IS NOT NULL
                        ORDER BY installed_rank
                        """,
                        String.class
                );

        assertThat(installedCount)
                .isEqualTo(16);

        assertThat(versions)
                .containsExactly(
                        "1",
                        "2",
                        "3",
                        "4",
                        "5",
                        "6",
                        "7",
                        "8",
                        "9",
                        "10",
                        "11",
                        "12",
                        "13",
                        "14",
                        "15",
                        "16"
                );
    }

    @Test
    void databaseConstraintsRejectInvalidData() {
        UUID firstUserId =
                UUID.randomUUID();

        UUID duplicateUserId =
                UUID.randomUUID();

        String duplicateEmail =
                "constraint-" + UUID.randomUUID()
                        + "@launchforge.dev";

        try {
            jdbcTemplate.update(
                    """
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
                    VALUES (?, ?, ?, ?, ?, ?, now(), now())
                    """,
                    firstUserId,
                    duplicateEmail,
                    "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm",
                    "Constraint",
                    "User",
                    true
            );

            assertThatThrownBy(
                    () ->
                            jdbcTemplate.update(
                                    """
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
                                    VALUES (?, ?, ?, ?, ?, ?, now(), now())
                                    """,
                                    duplicateUserId,
                                    duplicateEmail,
                                    "$2b$10$TaOdj1f1BBxImQSlxtsTiuRSp74bmAn12yBP3WE3tasMQUzAr/yRm",
                                    "Duplicate",
                                    "User",
                                    true
                            )
            )
                    .isInstanceOf(
                            DataIntegrityViolationException.class
                    );
        } finally {
            jdbcTemplate.update(
                    "DELETE FROM users WHERE id IN (?, ?)",
                    firstUserId,
                    duplicateUserId
            );
        }

        assertThatThrownBy(
                () ->
                        jdbcTemplate.update(
                                """
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
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                                """,
                                UUID.randomUUID(),
                                "LF-INVALID-" + UUID.randomUUID(),
                                "Invalid Product",
                                "invalid-product-" + UUID.randomUUID(),
                                "Invalid product used by integration test",
                                1L,
                                new BigDecimal("-1.00"),
                                true
                        )
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );

        UUID existingProductId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM products
                        ORDER BY sku
                        LIMIT 1
                        """,
                        UUID.class
                );

        assertThat(existingProductId)
                .isNotNull();

        assertThatThrownBy(
                () ->
                        jdbcTemplate.update(
                                """
                                INSERT INTO inventory (
                                    id,
                                    product_id,
                                    available_quantity,
                                    reserved_quantity,
                                    version,
                                    updated_at
                                )
                                VALUES (?, ?, ?, ?, ?, now())
                                """,
                                UUID.randomUUID(),
                                existingProductId,
                                -1,
                                0,
                                0L
                        )
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );
    }

    @Test
    @Transactional
    void seededProductsExposeTheirCategoryRelationship() {
        UUID productId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM products
                        ORDER BY sku
                        LIMIT 1
                        """,
                        UUID.class
                );

        assertThat(productId)
                .isNotNull();

        Product product =
                entityManager.find(
                        Product.class,
                        productId
                );

        assertThat(product)
                .isNotNull();

        assertThat(product.getSku())
                .isNotBlank();

        assertThat(product.getName())
                .isNotBlank();

        assertThat(product.getCategory())
                .isNotNull();

        assertThat(product.getCategory().getName())
                .isNotBlank();
    }

    @Test
    @Transactional
    void inventoryUsesVersionForOptimisticLocking() {
        UUID inventoryId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM inventory
                        ORDER BY product_id
                        LIMIT 1
                        """,
                        UUID.class
                );

        assertThat(inventoryId)
                .isNotNull();

        Inventory inventory =
                entityManager.find(
                        Inventory.class,
                        inventoryId
                );

        assertThat(inventory)
                .isNotNull();

        Long initialVersion =
                inventory.getVersion();

        Integer initialReservedQuantity =
                inventory.getReservedQuantity();

        inventory.setReservedQuantity(
                initialReservedQuantity + 1
        );

        entityManager.flush();
        entityManager.clear();

        Inventory reloaded =
                entityManager.find(
                        Inventory.class,
                        inventoryId
                );

        assertThat(reloaded)
                .isNotNull();

        assertThat(reloaded.getVersion())
                .isEqualTo(
                        initialVersion + 1
                );

        assertThat(reloaded.getReservedQuantity())
                .isEqualTo(
                        initialReservedQuantity + 1
                );
    }

    @Test
    void discountSeedSupportsFrequentCustomerRule() {
        Map<String, Object> discount =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            code,
                            minimum_orders,
                            lookback_months
                        FROM discount_configuration
                        WHERE code = 'FREQUENT_CUSTOMER'
                        """
                );

        assertThat(discount)
                .containsEntry(
                        "code",
                        "FREQUENT_CUSTOMER"
                );

        assertThat(discount)
                .containsEntry(
                        "minimum_orders",
                        5
                );

        assertThat(discount)
                .containsEntry(
                        "lookback_months",
                        12
                );
    }

    @Test
    void randomDiscountConfigurationIsRestored() {
        Map<String, Object> discount =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            code,
                            enabled,
                            percentage
                        FROM discount_configuration
                        WHERE code = 'RANDOM_ORDER'
                        """
                );

        assertThat(discount)
                .containsEntry(
                        "code",
                        "RANDOM_ORDER"
                );

        assertThat(discount)
                .containsEntry(
                        "enabled",
                        false
                );

        assertThat(
                (BigDecimal) discount.get("percentage")
        ).isEqualByComparingTo(
                "50.00"
        );
    }
}

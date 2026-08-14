package com.launchforge.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.inventory.InsufficientInventoryException;
import com.launchforge.persistence.model.inventory.Inventory;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryDomainTest {

    @Test
    void decreasesAvailableQuantity() {
        Inventory inventory = inventoryWithAvailable(5);

        inventory.decrease(2);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void decreasesAvailableQuantityToZero() {
        Inventory inventory = inventoryWithAvailable(2);

        inventory.decrease(2);

        assertThat(inventory.getAvailableQuantity()).isZero();
    }

    @Test
    void rejectsInsufficientInventory() {
        Inventory inventory = inventoryWithAvailable(1);

        assertThatThrownBy(() -> inventory.decrease(2))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Available: 1, requested: 2");
        assertThat(inventory.getAvailableQuantity()).isEqualTo(1);
    }

    @Test
    void increasesAvailableQuantity() {
        Inventory inventory = inventoryWithAvailable(3);

        inventory.increase(4);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    void neverAllowsNonPositiveQuantityOperations() {
        Inventory inventory = inventoryWithAvailable(3);

        assertThatThrownBy(() -> inventory.increase(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
        assertThatThrownBy(() -> inventory.decrease(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
        assertThat(inventory.getAvailableQuantity()).isEqualTo(3);
    }

    private Inventory inventoryWithAvailable(int availableQuantity) {
        Product product = new Product();
        product.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(availableQuantity);
        inventory.setReservedQuantity(0);
        inventory.setVersion(0L);
        return inventory;
    }
}

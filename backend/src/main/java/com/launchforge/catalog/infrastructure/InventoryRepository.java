package com.launchforge.catalog.infrastructure;

import com.launchforge.persistence.model.inventory.Inventory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    void deleteByProduct_Id(UUID productId);

    Optional<Inventory> findByProduct_Id(UUID productId);
}

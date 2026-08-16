package com.launchforge.catalog.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.launchforge.persistence.model.inventory.Inventory;

public interface InventoryRepository
        extends JpaRepository<Inventory, UUID> {

    void deleteByProduct_Id(UUID productId);

    @Override
    @SuppressWarnings("null")
    @EntityGraph(attributePaths = "product")
    Page<Inventory> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Optional<Inventory> findByProduct_Id(UUID productId);
}

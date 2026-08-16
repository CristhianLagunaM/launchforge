package com.launchforge.catalog.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.launchforge.persistence.model.catalog.Product;

public interface ProductRepository
        extends JpaRepository<Product, UUID>,
                JpaSpecificationExecutor<Product> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    @Override
    @SuppressWarnings("null")
    @EntityGraph(attributePaths = {"category", "inventory"})
    Optional<Product> findById(UUID id);

    @Override
    @SuppressWarnings("null")
    @EntityGraph(attributePaths = {"category", "inventory"})
    Page<Product> findAll(
            Specification<Product> specification,
            Pageable pageable
    );
}

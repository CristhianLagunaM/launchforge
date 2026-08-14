package com.launchforge.catalog.infrastructure;

import com.launchforge.persistence.model.catalog.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    @Override
    @EntityGraph(attributePaths = {"category", "inventory"})
    Optional<Product> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"category", "inventory"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);
}

package com.launchforge.catalog.infrastructure;

import com.launchforge.catalog.application.ProductSearchCriteria;
import com.launchforge.persistence.model.catalog.Category;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.inventory.Inventory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withCriteria(ProductSearchCriteria criteria, boolean includeInactive) {
        return (root, query, builder) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(criteria.name())) {
                predicates.add(builder.like(
                        builder.lower(root.get("name")),
                        "%" + normalize(criteria.name()) + "%"
                ));
            }

            if (hasText(criteria.sku())) {
                predicates.add(builder.like(
                        builder.lower(root.get("sku")),
                        "%" + normalize(criteria.sku()) + "%"
                ));
            }

            if (hasText(criteria.category())) {
                Join<Product, Category> categoryJoin = root.join("category", JoinType.INNER);
                String normalizedCategory = normalize(criteria.category());
                predicates.add(builder.or(
                        builder.equal(builder.lower(categoryJoin.get("name")), normalizedCategory),
                        builder.equal(builder.lower(categoryJoin.get("slug")), normalizedCategory)
                ));
            }

            if (criteria.minPrice() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }

            if (criteria.maxPrice() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }

            if (includeInactive) {
                if (criteria.active() != null) {
                    predicates.add(builder.equal(root.get("active"), criteria.active()));
                }
            } else {
                predicates.add(builder.isTrue(root.get("active")));
            }

            if (criteria.available() != null) {
                Join<Product, Inventory> inventoryJoin = root.join("inventory", JoinType.INNER);
                if (Boolean.TRUE.equals(criteria.available())) {
                    predicates.add(builder.greaterThan(inventoryJoin.get("availableQuantity"), 0));
                } else {
                    predicates.add(builder.equal(inventoryJoin.get("availableQuantity"), 0));
                }
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

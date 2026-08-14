package com.launchforge.catalog.application;

import com.launchforge.catalog.api.dto.CategoryResponse;
import com.launchforge.catalog.api.dto.ProductResponse;
import com.launchforge.persistence.model.catalog.Category;
import com.launchforge.persistence.model.catalog.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                toCategoryView(product.getCategory()),
                product.getPrice(),
                Boolean.TRUE.equals(product.getActive()),
                product.getInventory() != null && product.getInventory().getAvailableQuantity() > 0,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                Boolean.TRUE.equals(category.getActive())
        );
    }

    private ProductResponse.CategoryView toCategoryView(Category category) {
        return new ProductResponse.CategoryView(
                category.getId(),
                category.getName(),
                category.getSlug(),
                Boolean.TRUE.equals(category.getActive())
        );
    }
}

package com.launchforge.catalog.api;

import com.launchforge.catalog.api.dto.CategoryResponse;
import com.launchforge.catalog.application.ProductCatalogService;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final ProductCatalogService productCatalogService;

    public CategoryController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    public List<CategoryResponse> listCategories(Authentication authentication) {
        return productCatalogService.listCategories(isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        Collection<?> authorities = authentication.getAuthorities();
        return authorities.stream().anyMatch(authority -> authority.toString().equals("ROLE_ADMIN"));
    }
}

package com.launchforge.catalog.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.launchforge.catalog.api.dto.ProductResponse;
import com.launchforge.catalog.api.dto.ProductStatusRequest;
import com.launchforge.catalog.api.dto.ProductUpsertRequest;
import com.launchforge.catalog.application.ProductCatalogService;
import com.launchforge.catalog.application.ProductSearchCriteria;
import com.launchforge.shared.exception.ApiBadRequestException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    public Page<ProductResponse> listProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(required = false) String sort,
            Authentication authentication
    ) {
        Pageable pageable = toPageable(page, size, sort);

        ProductSearchCriteria criteria = new ProductSearchCriteria(
                name,
                sku,
                category,
                minPrice,
                maxPrice,
                active,
                available
        );

        return productCatalogService.listProducts(
                criteria,
                pageable,
                isAdmin(authentication)
        );
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return productCatalogService.getProduct(
                id,
                isAdmin(authentication)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse createProduct(
            @Valid @RequestBody ProductUpsertRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return productCatalogService.createProduct(
                request,
                UUID.fromString(jwt.getSubject())
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpsertRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return productCatalogService.updateProduct(
                id,
                request,
                UUID.fromString(jwt.getSubject())
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse changeProductStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ProductStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return productCatalogService.changeStatus(
                id,
                request,
                UUID.fromString(jwt.getSubject())
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        productCatalogService.deleteProduct(
                id,
                UUID.fromString(jwt.getSubject())
        );
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        Collection<?> authorities = authentication.getAuthorities();

        return authorities.stream()
                .anyMatch(authority ->
                        authority.toString().equals("ROLE_ADMIN")
                );
    }

    private Pageable toPageable(
            int page,
            int size,
            String sortParam
    ) {
        if (sortParam == null || sortParam.isBlank()) {
            return PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Order.asc("name"))
            );
        }

        String[] tokens = sortParam.split(",", 2);

        String property = Objects.requireNonNull(
                mapSortProperty(tokens[0]),
                "Sort property must not be null"
        );

        Sort.Direction direction = Sort.Direction.ASC;

        if (tokens.length == 2) {
            String directionValue = Objects.requireNonNull(
                    tokens[1]
                            .trim()
                            .toUpperCase(Locale.ROOT),
                    "Sort direction must not be null"
            );

            direction = Sort.Direction
                    .fromOptionalString(directionValue)
                    .orElseThrow(() -> new ApiBadRequestException(
                            "Invalid sort direction",
                            "Invalid sort direction for: " + sortParam,
                            "catalog/invalid-sort"
                    ));
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(new Sort.Order(direction, property))
        );
    }

    private String mapSortProperty(String property) {
        String normalizedProperty = property.trim();

        return switch (normalizedProperty) {
            case "name",
                    "sku",
                    "slug",
                    "price",
                    "active",
                    "createdAt",
                    "updatedAt" -> normalizedProperty;

            case "category" -> "category.name";

            case "available" -> "inventory.availableQuantity";

            default -> throw new ApiBadRequestException(
                    "Invalid sort field",
                    "Unsupported sort field: " + property,
                    "catalog/invalid-sort"
            );
        };
    }
}

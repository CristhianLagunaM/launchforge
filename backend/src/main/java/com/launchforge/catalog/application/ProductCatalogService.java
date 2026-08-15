package com.launchforge.catalog.application;

import com.launchforge.catalog.api.dto.CategoryResponse;
import com.launchforge.catalog.api.dto.ProductResponse;
import com.launchforge.catalog.api.dto.ProductStatusRequest;
import com.launchforge.catalog.api.dto.ProductUpsertRequest;
import com.launchforge.catalog.infrastructure.CategoryRepository;
import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.catalog.infrastructure.OrderItemRepository;
import com.launchforge.catalog.infrastructure.ProductRepository;
import com.launchforge.catalog.infrastructure.ProductSpecifications;
import com.launchforge.persistence.model.catalog.Category;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.inventory.Inventory;
import com.launchforge.shared.exception.ApiBadRequestException;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;

    public ProductCatalogService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            InventoryRepository inventoryRepository,
            OrderItemRepository orderItemRepository,
            ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(ProductSearchCriteria criteria, Pageable pageable, boolean includeInactive) {
        validatePriceRange(criteria);
        return productRepository.findAll(ProductSpecifications.withCriteria(criteria, includeInactive), pageable)
                .map(productMapper::toProductResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId, boolean includeInactive) {
        Product product = loadProduct(productId);
        if (!includeInactive && !Boolean.TRUE.equals(product.getActive())) {
            throw new ApiNotFoundException(
                    "Product not found",
                    "Product not found for id: " + productId,
                    "catalog/product-not-found"
            );
        }
        return productMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(boolean includeInactive) {
        List<Category> categories = includeInactive
                ? categoryRepository.findAll().stream().sorted(java.util.Comparator.comparing(Category::getName)).toList()
                : categoryRepository.findAllByActiveTrueOrderByNameAsc();
        return categories.stream()
                .map(productMapper::toCategoryResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductUpsertRequest request, UUID actorUserId) {
        validateUniqueFields(request.sku(), request.slug(), null);
        Category category = loadCategory(request.categoryId());

        Product product = new Product();
        applyUpsert(product, request, category, actorUserId, true);
        Product savedProduct = saveProduct(product);
        ensureInventoryExists(savedProduct);
        return productMapper.toProductResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductUpsertRequest request, UUID actorUserId) {
        Product product = loadProduct(productId);
        validateUniqueFields(request.sku(), request.slug(), productId);
        Category category = loadCategory(request.categoryId());

        applyUpsert(product, request, category, actorUserId, false);
        return productMapper.toProductResponse(saveProduct(product));
    }

    @Transactional
    public ProductResponse changeStatus(UUID productId, ProductStatusRequest request, UUID actorUserId) {
        Product product = loadProduct(productId);
        product.setActive(request.active());
        product.setUpdatedBy(actorUserId);
        return productMapper.toProductResponse(saveProduct(product));
    }

    @Transactional
    public void deleteProduct(UUID productId, UUID actorUserId) {
        Product product = loadProduct(productId);
        if (orderItemRepository.existsByProduct_Id(productId)) {
            product.setActive(false);
            product.setUpdatedBy(actorUserId);
            saveProduct(product);
            return;
        }

        inventoryRepository.deleteByProduct_Id(productId);
        productRepository.delete(product);
    }

    private Product saveProduct(Product product) {
        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiConflictException(
                    "Product conflict",
                    "Product SKU or slug already exists.",
                    "catalog/product-conflict"
            );
        }
    }

    private void applyUpsert(
            Product product,
            ProductUpsertRequest request,
            Category category,
            UUID actorUserId,
            boolean creating
    ) {
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setSlug(request.slug().trim());
        product.setDescription(request.description().trim());
        product.setCategory(category);
        product.setPrice(request.price());
        if (creating) {
            product.setActive(Boolean.TRUE);
            product.setCreatedBy(actorUserId);
        }
        product.setUpdatedBy(actorUserId);
    }

    private void ensureInventoryExists(Product product) {
        if (inventoryRepository.findByProduct_Id(product.getId()).isPresent()) {
            return;
        }

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(0);
        inventory.setReservedQuantity(0);
        product.setInventory(inventory);
        inventoryRepository.save(inventory);
    }

    private void validatePriceRange(ProductSearchCriteria criteria) {
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.maxPrice().compareTo(criteria.minPrice()) < 0) {
            throw new ApiBadRequestException(
                    "Invalid price range",
                    "maxPrice must be greater than or equal to minPrice.",
                    "catalog/invalid-price-range"
            );
        }
    }

    private void validateUniqueFields(String sku, String slug, UUID currentProductId) {
        boolean duplicatedSku = currentProductId == null
                ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, currentProductId);
        if (duplicatedSku) {
            throw new ApiConflictException(
                    "Duplicate SKU",
                    "A product already exists with SKU: " + sku,
                    "catalog/duplicate-sku"
            );
        }

        boolean duplicatedSlug = currentProductId == null
                ? productRepository.existsBySlugIgnoreCase(slug)
                : productRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentProductId);
        if (duplicatedSlug) {
            throw new ApiConflictException(
                    "Duplicate slug",
                    "A product already exists with slug: " + slug,
                    "catalog/duplicate-slug"
            );
        }
    }

    private Product loadProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Product not found",
                        "Product not found for id: " + productId,
                        "catalog/product-not-found"
                ));
    }

    private Category loadCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "Category not found",
                        "Category not found for id: " + categoryId,
                        "catalog/category-not-found"
                ));
    }
}

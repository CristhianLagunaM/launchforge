package com.launchforge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.launchforge.catalog.api.dto.ProductStatusRequest;
import com.launchforge.catalog.api.dto.ProductUpsertRequest;
import com.launchforge.catalog.application.ProductCatalogService;
import com.launchforge.catalog.application.ProductMapper;
import com.launchforge.catalog.infrastructure.CategoryRepository;
import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.catalog.infrastructure.OrderItemRepository;
import com.launchforge.catalog.infrastructure.ProductRepository;
import com.launchforge.persistence.model.catalog.Category;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.inventory.Inventory;
import com.launchforge.shared.exception.ApiConflictException;
import com.launchforge.shared.exception.ApiNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProductCatalogServiceTest {

    private static final UUID PRODUCT_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222221"
            );

    private static final UUID ACTOR_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private ProductCatalogService productCatalogService;

    @BeforeEach
    public void setUp() {
        productCatalogService = new ProductCatalogService(
                productRepository,
                categoryRepository,
                inventoryRepository,
                orderItemRepository,
                new ProductMapper()
        );
    }

    @Test
    void createsProductWhenRequestIsValid() {
        ProductUpsertRequest request =
                new ProductUpsertRequest(
                        "LF-NEW-001",
                        "New Product",
                        "new-product",
                        "Useful package",
                        1L,
                        new BigDecimal("1500.00")
                );

        Category category = buildCategory();

        Product saved = buildProduct();
        saved.setSku(request.sku());
        saved.setName(request.name());
        saved.setSlug(request.slug());
        saved.setDescription(request.description());
        saved.setPrice(request.price());
        saved.setCategory(category);

        when(
                categoryRepository.findById(1L)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                productRepository.save(
                        any(Product.class)
                )
        ).thenReturn(
                saved
        );

        var response =
                productCatalogService.createProduct(
                        request,
                        ACTOR_ID
                );

        assertThat(
                response.sku()
        ).isEqualTo(
                "LF-NEW-001"
        );

        assertThat(
                response.slug()
        ).isEqualTo(
                "new-product"
        );

        assertThat(
                response.category().id()
        ).isEqualTo(
                1L
        );
    }

    @Test
    void rejectsDuplicateSku() {
        ProductUpsertRequest request =
                new ProductUpsertRequest(
                        "LF-LANDING-001",
                        "Another Product",
                        "another-product",
                        "Useful package",
                        1L,
                        new BigDecimal("1500.00")
                );

        when(
                productRepository.existsBySkuIgnoreCase(
                        "LF-LANDING-001"
                )
        ).thenReturn(
                true
        );

        assertThatThrownBy(
                () ->
                        productCatalogService.createProduct(
                                request,
                                ACTOR_ID
                        )
        )
                .isInstanceOf(
                        ApiConflictException.class
                )
                .hasMessageContaining(
                        "SKU"
                );
    }

    @Test
    void rejectsDuplicateSlug() {
        ProductUpsertRequest request =
                new ProductUpsertRequest(
                        "LF-NEW-001",
                        "Another Product",
                        "landing-page-launch",
                        "Useful package",
                        1L,
                        new BigDecimal("1500.00")
                );

        when(
                productRepository.existsBySlugIgnoreCase(
                        "landing-page-launch"
                )
        ).thenReturn(
                true
        );

        assertThatThrownBy(
                () ->
                        productCatalogService.createProduct(
                                request,
                                ACTOR_ID
                        )
        )
                .isInstanceOf(
                        ApiConflictException.class
                )
                .hasMessageContaining(
                        "slug"
                );
    }

    @Test
    void updatesExistingProduct() {
        Product existing = buildProduct();
        Category category = buildCategory();

        ProductUpsertRequest request =
                new ProductUpsertRequest(
                        "LF-LANDING-002",
                        "Landing Page Premium",
                        "landing-page-premium",
                        "Updated package",
                        1L,
                        new BigDecimal("1900.00")
                );

        when(
                productRepository.findById(PRODUCT_ID)
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                categoryRepository.findById(1L)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                productRepository.save(
                        any(Product.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        var response =
                productCatalogService.updateProduct(
                        PRODUCT_ID,
                        request,
                        ACTOR_ID
                );

        assertThat(
                response.name()
        ).isEqualTo(
                "Landing Page Premium"
        );

        assertThat(
                response.price()
        ).isEqualByComparingTo(
                "1900.00"
        );

        assertThat(
                response.slug()
        ).isEqualTo(
                "landing-page-premium"
        );
    }

    @Test
    void disablesProductViaStatusChange() {
        Product existing = buildProduct();

        when(
                productRepository.findById(PRODUCT_ID)
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                productRepository.save(
                        any(Product.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        var response =
                productCatalogService.changeStatus(
                        PRODUCT_ID,
                        new ProductStatusRequest(false),
                        ACTOR_ID
                );

        assertThat(
                response.active()
        ).isFalse();
    }

    @Test
    void throwsNotFoundWhenProductDoesNotExist() {
        when(
                productRepository.findById(PRODUCT_ID)
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(
                () ->
                        productCatalogService.getProduct(
                                PRODUCT_ID,
                                true
                        )
        ).isInstanceOf(
                ApiNotFoundException.class
        );
    }

    @Test
    void deleteSoftDisablesProductsWithCommercialHistory() {
        Product existing = buildProduct();

        when(
                productRepository.findById(PRODUCT_ID)
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                orderItemRepository.existsByProduct_Id(
                        PRODUCT_ID
                )
        ).thenReturn(
                true
        );

        when(
                productRepository.save(
                        any(Product.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        productCatalogService.deleteProduct(
                PRODUCT_ID,
                ACTOR_ID
        );

        assertThat(
                existing.getActive()
        ).isFalse();

        verify(
                inventoryRepository,
                never()
        ).deleteByProduct_Id(
                PRODUCT_ID
        );
    }

    private Product buildProduct() {
        Product product = new Product();

        product.setId(PRODUCT_ID);
        product.setSku("LF-LANDING-001");
        product.setName("Landing Page Launch");
        product.setSlug("landing-page-launch");
        product.setDescription(
                "High-conversion landing page."
        );
        product.setPrice(
                new BigDecimal("1200.00")
        );
        product.setActive(true);
        product.setCategory(
                buildCategory()
        );
        product.setCreatedBy(ACTOR_ID);
        product.setUpdatedBy(ACTOR_ID);

        Inventory inventory =
                new Inventory();

        inventory.setAvailableQuantity(8);
        inventory.setReservedQuantity(1);
        inventory.setProduct(product);

        product.setInventory(inventory);

        return product;
    }

    private Category buildCategory() {
        Category category =
                new Category();

        category.setId(1L);
        category.setName("WEB");
        category.setSlug("web");
        category.setActive(true);

        return category;
    }
}

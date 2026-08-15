package com.launchforge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.launchforge.catalog.api.dto.ProductResponse;
import com.launchforge.catalog.application.ProductCatalogService;
import com.launchforge.catalog.application.ProductSearchCriteria;
import com.launchforge.catalog.infrastructure.CategoryRepository;
import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.catalog.infrastructure.ProductRepository;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import com.launchforge.persistence.model.catalog.Category;
import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.persistence.model.inventory.Inventory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductCatalogIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void searchesByName() {
        Page<ProductResponse> page = productCatalogService.listProducts(
                new ProductSearchCriteria("landing", null, null, null, null, null, null),
                PageRequest.of(0, 20, Sort.by("name")),
                true
        );

        assertThat(page.getContent()).extracting(ProductResponse::name)
                .contains("Landing Page Launch");
    }

    @Test
    void searchesByCategory() {
        Page<ProductResponse> page = productCatalogService.listProducts(
                new ProductSearchCriteria(null, null, "WEB", null, null, null, null),
                PageRequest.of(0, 20, Sort.by("name")),
                true
        );

        assertThat(page.getContent()).extracting(product -> product.category().name())
                .containsOnly("WEB");
    }

    @Test
    void searchesByPriceRange() {
        Page<ProductResponse> page = productCatalogService.listProducts(
                new ProductSearchCriteria(null, null, null, new BigDecimal("1000.00"), new BigDecimal("2000.00"), null, null),
                PageRequest.of(0, 20, Sort.by("price")),
                true
        );

        assertThat(page.getContent()).allSatisfy(product -> {
            assertThat(product.price()).isGreaterThanOrEqualTo(new BigDecimal("1000.00"));
            assertThat(product.price()).isLessThanOrEqualTo(new BigDecimal("2000.00"));
        });
    }

    @Test
    @Transactional
    void filtersByActiveFlag() {
        persistInactiveProduct();

        Page<ProductResponse> page = productCatalogService.listProducts(
                new ProductSearchCriteria(null, null, null, null, null, false, null),
                PageRequest.of(0, 20, Sort.by("name")),
                true
        );

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allSatisfy(product -> assertThat(product.active()).isFalse());
    }

    @Test
    void combinesMultipleFilters() {
        Page<ProductResponse> page = productCatalogService.listProducts(
                new ProductSearchCriteria("maintenance", "LF-MNT", "maintenance", new BigDecimal("500.00"), new BigDecimal("900.00"), true, true),
                PageRequest.of(0, 20, Sort.by("name")),
                true
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().sku()).isEqualTo("LF-MNT-001");
    }

    @Test
    void paginatesInDatabase() {
        Page<ProductResponse> firstPage = productCatalogService.listProducts(
                new ProductSearchCriteria(null, null, null, null, null, null, null),
                PageRequest.of(0, 2, Sort.by("name")),
                true
        );
        Page<ProductResponse> secondPage = productCatalogService.listProducts(
                new ProductSearchCriteria(null, null, null, null, null, null, null),
                PageRequest.of(1, 2, Sort.by("name")),
                true
        );

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent().getFirst().id()).isNotEqualTo(secondPage.getContent().getFirst().id());
        assertThat(firstPage.getTotalElements()).isGreaterThan(2);
    }

    @Test
    void sortsResultsByPriceDescending() {
        Page<ProductResponse> page = productCatalogService.listProducts(
                new ProductSearchCriteria(null, null, null, null, null, null, null),
                PageRequest.of(0, 5, Sort.by(Sort.Order.desc("price"))),
                true
        );

        assertThat(page.getContent().getFirst().sku()).isEqualTo("LF-SAAS-001");
    }

    private void persistInactiveProduct() {
        Category category = categoryRepository.findById(1L).orElseThrow();

        Product product = new Product();
        product.setSku("LF-INACTIVE-001");
        product.setName("Inactive Product");
        product.setSlug("inactive-product");
        product.setDescription("Inactive seeded for test");
        product.setCategory(category);
        product.setPrice(new BigDecimal("999.00"));
        product.setActive(false);
        product.setCreatedAt(Instant.parse("2026-08-14T12:00:00Z"));
        product.setUpdatedAt(Instant.parse("2026-08-14T12:00:00Z"));
        productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setUpdatedAt(Instant.parse("2026-08-14T12:00:00Z"));
        inventoryRepository.save(inventory);
    }
}

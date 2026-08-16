package com.launchforge.persistence.model.inventory;

import java.time.Instant;

import com.launchforge.persistence.model.catalog.Product;
import com.launchforge.shared.persistence.AbstractUuidEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "inventory")
public class Inventory extends AbstractUuidEntity {

    private static final String QUANTITY_MUST_BE_POSITIVE = "Quantity must be greater than zero.";

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer availableQuantity;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer reservedQuantity;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (version == null) {
            version = 0L;
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void increase(int quantity) {
        validatePositiveQuantity(quantity);
        availableQuantity += quantity;
    }

    public void decrease(int quantity) {
        validatePositiveQuantity(quantity);
        if (availableQuantity < quantity) {
            throw new InsufficientInventoryException(product.getId(), availableQuantity, quantity);
        }
        availableQuantity -= quantity;
    }

    public void restore(int quantity) {
        increase(quantity);
    }

    public void reserve(int quantity) {
        validatePositiveQuantity(quantity);
        if (availableQuantity < quantity) throw new InsufficientInventoryException(product.getId(), availableQuantity, quantity);
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void confirmReservation(int quantity) {
        validatePositiveQuantity(quantity);
        if (reservedQuantity < quantity) throw new IllegalStateException("Not enough reserved capacity.");
        reservedQuantity -= quantity;
    }

    public void releaseReservation(int quantity) {
        validatePositiveQuantity(quantity);
        if (reservedQuantity < quantity) throw new IllegalStateException("Not enough reserved capacity.");
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(QUANTITY_MUST_BE_POSITIVE);
        }
    }
}

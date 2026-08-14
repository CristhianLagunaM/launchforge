package com.launchforge.persistence.model.discounts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.launchforge.shared.persistence.AbstractUuidEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "discount_configuration")
public class DiscountConfiguration extends AbstractUuidEntity {

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String type;

    @NotNull
    @Column(nullable = false)
    private Boolean enabled;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 3, fraction = 2)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column
    private Instant startAt;

    @Column
    private Instant endAt;

    @Positive
    @Column
    private Integer minimumOrders;

    @Positive
    @Column
    private Integer lookbackMonths;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private UUID updatedBy;

    @OneToMany(mappedBy = "discountConfiguration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderDiscount> orderDiscounts = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public Integer getMinimumOrders() {
        return minimumOrders;
    }

    public void setMinimumOrders(Integer minimumOrders) {
        this.minimumOrders = minimumOrders;
    }

    public Integer getLookbackMonths() {
        return lookbackMonths;
    }

    public void setLookbackMonths(Integer lookbackMonths) {
        this.lookbackMonths = lookbackMonths;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Set<OrderDiscount> getOrderDiscounts() {
        return orderDiscounts;
    }

    public void setOrderDiscounts(Set<OrderDiscount> orderDiscounts) {
        this.orderDiscounts = orderDiscounts;
    }
}

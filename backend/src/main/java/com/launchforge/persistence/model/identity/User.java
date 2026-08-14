package com.launchforge.persistence.model.identity;

import java.util.LinkedHashSet;
import java.util.Set;

import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.shared.persistence.AbstractAuditableEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User extends AbstractAuditableEntity {

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String firstName;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String lastName;

    @NotNull
    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private Set<CustomerOrder> orders = new LinkedHashSet<>();

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    public Set<CustomerOrder> getOrders() {
        return orders;
    }

    public void setOrders(Set<CustomerOrder> orders) {
        this.orders = orders;
    }
}

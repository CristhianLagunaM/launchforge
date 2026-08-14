package com.launchforge.shared.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

@MappedSuperclass
public abstract class AbstractUuidEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @PrePersist
    protected void assignIdIfMissing() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}

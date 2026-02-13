package com.ramil.booking.resource_booking.domain.resource.entity;

import java.util.UUID;

import com.ramil.booking.resource_booking.domain.common.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Setter;

@Entity
@Table(name = "resource")
public class ResourceEntity extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Setter
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Setter
    @Column(name = "description")
    private String description;

    @Setter
    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ResourceEntity() {
    }

    public ResourceEntity(UUID id, String name, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

}

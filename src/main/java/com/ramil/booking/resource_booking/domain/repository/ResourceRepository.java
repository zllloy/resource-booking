package com.ramil.booking.resource_booking.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramil.booking.resource_booking.domain.entity.ResourceEntity;

public interface ResourceRepository extends JpaRepository<ResourceEntity, UUID> {
}

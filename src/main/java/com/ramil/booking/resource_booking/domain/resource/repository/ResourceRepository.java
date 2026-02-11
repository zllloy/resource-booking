package com.ramil.booking.resource_booking.domain.resource.repository;


import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;

public interface ResourceRepository extends JpaRepository<ResourceEntity, UUID> {
  List<ResourceEntity> findByActive(boolean active);
}
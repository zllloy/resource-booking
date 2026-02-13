package com.ramil.booking.resource_booking.domain.resource.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;

import jakarta.persistence.LockModeType;

public interface ResourceRepository extends JpaRepository<ResourceEntity, UUID> {

    List<ResourceEntity> findByActive(boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResourceEntity r where r.id = :id")
    Optional<ResourceEntity> findByIdForUpdate(@Param("id") UUID id);
}

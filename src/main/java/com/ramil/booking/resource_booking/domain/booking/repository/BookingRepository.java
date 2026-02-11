package com.ramil.booking.resource_booking.domain.booking.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    @Query("""
            select b
            from BookingEntity b
            where b.resource.id = :resourceId
              and b.status in :activeStatuses
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    List<BookingEntity> findConflicts(@Param("resourceId") UUID resourceId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("activeStatuses") List<BookingStatus> activeStatuses);
}

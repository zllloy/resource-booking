package com.ramil.booking.resource_booking.domain.booking.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    @Query("select b from BookingEntity b where b.user.id = :userId order by b.startTime desc")
    List<BookingEntity> findByUserId(@Param("userId") UUID userId);

    @Query("select b from BookingEntity b order by b.startTime desc")
    List<BookingEntity> findAllOrderByStartTimeDesc();

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResourceEntity r where r.id = :id")
    Optional<ResourceEntity> findByIdForUpdate(@Param("id") UUID id);
}

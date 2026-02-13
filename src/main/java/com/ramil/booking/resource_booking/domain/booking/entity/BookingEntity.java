package com.ramil.booking.resource_booking.domain.booking.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.common.persistence.AuditableEntity;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.user.entity.AppUserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "booking")
public class BookingEntity extends AuditableEntity {

    @Getter
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    @Setter
    @Getter
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Getter
    @Setter
    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BookingStatus status;

    @Getter
    @Setter
    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Getter
    @Setter
    @Column(name = "paid_by", length = 255)
    private String paidBy;

    protected BookingEntity() {
    }

    public BookingEntity(UUID id, AppUserEntity user, ResourceEntity resource,
            OffsetDateTime startTime, OffsetDateTime endTime,
            BookingStatus status) {
        this.id = id;
        this.user = user;
        this.resource = resource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

}

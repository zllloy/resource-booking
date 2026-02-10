package com.ramil.booking.resource_booking.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.ramil.booking.resource_booking.common.persistence.AuditableEntity;
import com.ramil.booking.resource_booking.domain.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.model.PaymentStatus;
import com.ramil.booking.resource_booking.domain.model.PaymentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
public class PaymentEntity extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "provider_payload", columnDefinition = "jsonb")
    private String providerPayload;

    protected PaymentEntity() {
    }

    public PaymentEntity(UUID id, BookingEntity booking, PaymentProvider provider,
            PaymentType type, PaymentStatus status,
            BigDecimal amount, String currency, String providerPayload) {
        this.id = id;
        this.booking = booking;
        this.provider = provider;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.providerPayload = providerPayload;
    }

    public UUID getId() {
        return id;
    }

    public BookingEntity getBooking() {
        return booking;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public PaymentType getType() {
        return type;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProviderPayload() {
        return providerPayload;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}

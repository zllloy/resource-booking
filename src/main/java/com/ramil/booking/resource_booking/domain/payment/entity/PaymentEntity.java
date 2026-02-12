package com.ramil.booking.resource_booking.domain.payment.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.common.persistence.AuditableEntity;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentStatus;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentType;
import jakarta.persistence.*;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

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

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_payload", columnDefinition = "jsonb")
    private JsonNode providerPayload;

    protected PaymentEntity() {
    }

    public PaymentEntity(UUID id,
                         BookingEntity booking,
                         PaymentProvider provider,
                         PaymentType type,
                         PaymentStatus status,
                         BigDecimal amount,
                         String currency,
                         JsonNode providerPayload) {
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

    public JsonNode getProviderPayload() {
        return providerPayload;
    }
}

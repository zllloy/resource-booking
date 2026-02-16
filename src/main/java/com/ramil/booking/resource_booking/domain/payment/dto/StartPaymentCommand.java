package com.ramil.booking.resource_booking.domain.payment.dto;

import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentType;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record StartPaymentCommand(
        UUID bookingId,
        PaymentProvider provider,
        PaymentType type,
        BigDecimal amount,
        String currency,
        String payloadJson
) {

    public StartPaymentCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }
}

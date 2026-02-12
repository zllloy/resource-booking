package com.ramil.booking.resource_booking.domain.payment.dto;

import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

public record StartPaymentCommand(
        UUID bookingId,
        PaymentProvider provider,
        PaymentType type,
        BigDecimal amount,
        String currency,
        String payloadJson
) {}

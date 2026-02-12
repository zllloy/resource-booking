package com.ramil.booking.resource_booking.domain.payment.dto;

import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentStatus;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentView(
        UUID id,
        UUID bookingId,
        PaymentProvider provider,
        PaymentType type,
        PaymentStatus status,
        BigDecimal amount,
        String currency
) {}

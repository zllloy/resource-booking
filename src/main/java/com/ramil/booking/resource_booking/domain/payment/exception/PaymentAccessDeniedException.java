package com.ramil.booking.resource_booking.domain.payment.exception;

import java.util.UUID;

public class PaymentAccessDeniedException extends RuntimeException {
    public PaymentAccessDeniedException(UUID bookingId) {
        super("Access denied to payments for booking: " + bookingId);
    }
}

package com.ramil.booking.resource_booking.domain.booking.exception;

import java.util.UUID;

public class BookingAccessDeniedException extends RuntimeException {
    public BookingAccessDeniedException(UUID bookingId) {
        super("Not allowed for booking: " + bookingId);
    }
}

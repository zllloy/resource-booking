package com.ramil.booking.resource_booking.domain.booking.exception;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(UUID id) {
        super("Booking not found: " + id);
    }
}

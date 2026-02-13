package com.ramil.booking.resource_booking.domain.booking.exception;

import java.util.UUID;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;

public class BookingStatusException extends RuntimeException {
    public BookingStatusException(UUID bookingId, BookingStatus current, String expected) {
        super("Invalid booking status for " + bookingId + ". Current=" + current + ", expected: " + expected);
    }
}
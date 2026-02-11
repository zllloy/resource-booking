package com.ramil.booking.resource_booking.domain.exception;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BookingConflictException extends RuntimeException {
  public BookingConflictException(UUID resourceId, OffsetDateTime start, OffsetDateTime end) {
    super("Booking conflict for resource=" + resourceId + " at [" + start + " - " + end + "]");
  }
}
package com.ramil.booking.resource_booking.domain.exception;

import java.time.OffsetDateTime;

public class BookingTimeRangeException extends RuntimeException {
  public BookingTimeRangeException(OffsetDateTime start, OffsetDateTime end) {
    super("Invalid time range: start=" + start + ", end=" + end);
  }
}
package com.ramil.booking.resource_booking.domain.booking.exception;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class BookingConflictException extends RuntimeException {

  private final UUID resourceId;
  private final OffsetDateTime startTime;
  private final OffsetDateTime endTime;

  public BookingConflictException(UUID resourceId, OffsetDateTime startTime, OffsetDateTime endTime) {
    super("Booking conflict for resource " + resourceId +
            " in range [" + startTime + " - " + endTime + "]");
    this.resourceId = resourceId;
    this.startTime = startTime;
    this.endTime = endTime;
  }

}

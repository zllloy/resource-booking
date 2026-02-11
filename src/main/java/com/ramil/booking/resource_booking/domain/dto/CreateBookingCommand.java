package com.ramil.booking.resource_booking.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingCommand(
    UUID userId,
    UUID resourceId,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) { }
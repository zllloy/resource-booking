package com.ramil.booking.resource_booking.domain.booking.dto;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record CreateBookingCommand(
        UUID resourceId,
        OffsetDateTime startTime,
        OffsetDateTime endTime) {

    public CreateBookingCommand {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
    }
}
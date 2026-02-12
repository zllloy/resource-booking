package com.ramil.booking.resource_booking.domain.booking.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;

public record BookingView(
        UUID id,
        UUID userId,
        UUID resourceId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BookingStatus status) {
}
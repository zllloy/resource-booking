package com.ramil.booking.resource_booking.domain.resource.dto;

import java.util.UUID;

// Представление ресурса для чтения
public record ResourceView(UUID id, String name, String description, boolean active) {
}

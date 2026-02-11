package com.ramil.booking.resource_booking.domain.resource.dto;

import java.util.UUID;

/**
 * Read-only projection of a resource.
 *
 * @param id          resource id
 * @param name        resource name
 * @param description resource description (nullable)
 * @param active      whether resource is active
 */
public record ResourceView(UUID id, String name, String description, boolean active) {
}

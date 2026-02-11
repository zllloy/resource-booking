package com.ramil.booking.resource_booking.domain.resource.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Command for updating an existing resource.
 *
 * @param id          non-null resource id
 * @param name        non-null new name
 * @param description optional new description
 */
public record UpdateResourceCommand(UUID id, String name, String description) {

    public UpdateResourceCommand {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
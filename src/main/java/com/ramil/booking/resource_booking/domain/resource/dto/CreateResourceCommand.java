package com.ramil.booking.resource_booking.domain.resource.dto;

import java.util.Objects;

/**
 * Command for creating a new resource.
 *
 * @param name        non-null resource name
 * @param description optional description
 */
public record CreateResourceCommand(String name, String description) {

    public CreateResourceCommand {
        Objects.requireNonNull(name, "name must not be null");
    }
}
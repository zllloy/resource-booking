package com.ramil.booking.resource_booking.domain.resource.dto;

import java.util.Objects;

// Команда для создания нового ресурса
public record CreateResourceCommand(String name, String description) {

    public CreateResourceCommand {
        Objects.requireNonNull(name, "name must not be null");
    }
}
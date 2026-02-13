package com.ramil.booking.resource_booking.domain.resource.exception;

public class InvalidResourceDescriptionException extends RuntimeException {
    public InvalidResourceDescriptionException(String message) {
        super(message);
    }

    public static InvalidResourceDescriptionException tooLong(int max) {
        return new InvalidResourceDescriptionException("Resource description must not be longer than " + max);
    }
}

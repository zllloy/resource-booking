package com.ramil.booking.resource_booking.domain.resource.exception;

public class InvalidResourceNameException extends RuntimeException {
    public InvalidResourceNameException(String message) {
        super(message);
    }

    public static InvalidResourceNameException blank() {
        return new InvalidResourceNameException("Resource name must not be blank");
    }

    public static InvalidResourceNameException tooLong(int max) {
        return new InvalidResourceNameException("Resource name must not be longer than " + max);
    }

}

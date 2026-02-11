package com.ramil.booking.resource_booking.domain.resource.exception;

import java.util.UUID;

public class ResourceInactiveException extends RuntimeException {
  public ResourceInactiveException(UUID resourceId) {
    super("Resource is inactive: " + resourceId);
  }
}
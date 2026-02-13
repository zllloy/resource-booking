package com.ramil.booking.resource_booking.domain.user.security;

import java.util.UUID;

public interface CurrentUserProvider {
    UUID currentUserId();
    boolean isAdmin();
    String currentUserEmail();
}

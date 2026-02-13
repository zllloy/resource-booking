package com.ramil.booking.resource_booking.config.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ramil.booking.resource_booking.domain.user.exception.UserNotFoundException;
import com.ramil.booking.resource_booking.domain.user.repository.AppUserRepository;
import com.ramil.booking.resource_booking.domain.user.security.CurrentUserProvider;

@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    private final AppUserRepository appUserRepository;

    public SpringSecurityCurrentUserProvider(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UUID currentUserId() {
        String email = currentUserEmail();
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }

    @Override
    public String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "system";
        }
        return auth.getName(); // email
    }

    @Override
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

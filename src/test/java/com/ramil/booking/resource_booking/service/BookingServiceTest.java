package com.ramil.booking.resource_booking.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ramil.booking.resource_booking.domain.dto.CreateBookingCommand;
import com.ramil.booking.resource_booking.domain.entity.AppUserEntity;
import com.ramil.booking.resource_booking.domain.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.exception.BookingConflictException;
import com.ramil.booking.resource_booking.domain.exception.ResourceInactiveException;
import com.ramil.booking.resource_booking.domain.model.Role;
import com.ramil.booking.resource_booking.domain.repository.AppUserRepository;
import com.ramil.booking.resource_booking.domain.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.repository.ResourceRepository;
import com.ramil.booking.resource_booking.domain.service.BookingService;

class BookingServiceTest {

  private final BookingRepository bookingRepository = mock(BookingRepository.class);
  private final ResourceRepository resourceRepository = mock(ResourceRepository.class);
  private final AppUserRepository appUserRepository = mock(AppUserRepository.class);

  private final BookingService bookingService = new BookingService(bookingRepository, resourceRepository,
      appUserRepository);

  @Test
  void createDraft_throws_if_resource_inactive() {
    UUID userId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();

    AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
    ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, false);

    when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
    when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

    OffsetDateTime start = OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime end = start.plusHours(1);

    assertThatThrownBy(() -> bookingService.createDraft(new CreateBookingCommand(userId, resourceId, start, end)))
        .isInstanceOf(ResourceInactiveException.class);

    verifyNoInteractions(bookingRepository);
  }

  @Test
  void createDraft_throws_if_conflict_exists() {
    UUID userId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();

    AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
    ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

    when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
    when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

    OffsetDateTime start = OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime end = start.plusHours(1);

    when(bookingRepository.findConflicts(eq(resourceId), eq(start), eq(end), anyList()))
        .thenReturn(List.of(mock(com.ramil.booking.resource_booking.domain.entity.BookingEntity.class)));

    assertThatThrownBy(() -> bookingService.createDraft(new CreateBookingCommand(userId, resourceId, start, end)))
        .isInstanceOf(BookingConflictException.class);

    verify(bookingRepository, never()).save(any());
  }
}
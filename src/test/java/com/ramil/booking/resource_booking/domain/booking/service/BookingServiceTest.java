package com.ramil.booking.resource_booking.domain.booking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ramil.booking.resource_booking.domain.booking.dto.CreateBookingCommand;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.ramil.booking.resource_booking.domain.booking.dto.BookingView;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingConflictException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingStatusException;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.resource.repository.ResourceRepository;
import com.ramil.booking.resource_booking.domain.user.entity.AppUserEntity;
import com.ramil.booking.resource_booking.domain.user.model.Role;
import com.ramil.booking.resource_booking.domain.user.repository.AppUserRepository;
import com.ramil.booking.resource_booking.domain.user.security.CurrentUserProvider;
import com.ramil.booking.resource_booking.domain.booking.mapper.BookingMapper;

class BookingServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final ResourceRepository resourceRepository = mock(ResourceRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final BookingMapper bookingMapper = mock(BookingMapper.class);

    private final BookingService bookingService =
            new BookingService(bookingRepository, resourceRepository, appUserRepository, currentUser, bookingMapper);

    @Test
    void markWaitingPayment_moves_draft_to_waitingPayment() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(userId);

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        OffsetDateTime start = OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.DRAFT
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.saveAndFlush(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        BookingView expectedView = new BookingView(bookingId, userId, resourceId, start, end, BookingStatus.WAITING_PAYMENT);
        when(bookingMapper.toView(any(BookingEntity.class))).thenReturn(expectedView);

        BookingView result = bookingService.markWaitingPayment(bookingId);

        assertThat(result.status()).isEqualTo(BookingStatus.WAITING_PAYMENT);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING_PAYMENT);

        verify(bookingRepository).saveAndFlush(booking);
    }

    @Test
    void markWaitingPayment_throws_if_status_not_draft() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(userId);

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        OffsetDateTime start = OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.WAITING_PAYMENT
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.markWaitingPayment(bookingId))
                .isInstanceOf(BookingStatusException.class)
                .hasMessageContaining("Current=WAITING_PAYMENT")
                .hasMessageContaining("expected: DRAFT");

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void markWaitingPayment_throws_conflict_when_db_constraint_fails() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(userId);

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        OffsetDateTime start = OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.DRAFT
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.saveAndFlush(any(BookingEntity.class)))
                .thenThrow(new DataIntegrityViolationException("booking_no_overlap"));

        assertThatThrownBy(() -> bookingService.markWaitingPayment(bookingId))
                .isInstanceOf(BookingConflictException.class);

        verify(bookingRepository).saveAndFlush(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING_PAYMENT);
    }

    @Test
    void createDraft_throws_if_time_range_invalid() {
        UUID userId = UUID.randomUUID();
        when(currentUser.currentUserId()).thenReturn(userId);

        OffsetDateTime start = OffsetDateTime.parse("2026-02-10T10:00:00Z");
        OffsetDateTime end = start;

        assertThatThrownBy(() ->
                bookingService.createDraft(new CreateBookingCommand(UUID.randomUUID(), start, end)))
                .isInstanceOf(com.ramil.booking.resource_booking.domain.booking.exception.BookingTimeRangeException.class);

        verifyNoInteractions(resourceRepository, appUserRepository, bookingRepository);
    }

    @Test
    void createDraft_throws_if_resource_not_found() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(currentUser.currentUserId()).thenReturn(userId);

        OffsetDateTime start = OffsetDateTime.parse("2026-02-10T10:00:00Z");
        OffsetDateTime end = start.plusHours(1);

        when(resourceRepository.findByIdForUpdate(resourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                bookingService.createDraft(new CreateBookingCommand(resourceId, start, end)))
                .isInstanceOf(com.ramil.booking.resource_booking.domain.resource.exception.ResourceNotFoundException.class);

        verify(resourceRepository).findByIdForUpdate(resourceId);
        verifyNoInteractions(appUserRepository, bookingRepository);
    }

    @Test
    void createDraft_creates_draft_for_current_user() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(currentUser.currentUserId()).thenReturn(userId);

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        when(resourceRepository.findByIdForUpdate(resourceId)).thenReturn(Optional.of(resource));
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findConflicts(eq(resourceId), any(), any(), anyList())).thenReturn(List.of());
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime start = OffsetDateTime.parse("2026-02-10T10:00:00Z");
        OffsetDateTime end = start.plusHours(1);

        BookingView view = bookingService.createDraft(new CreateBookingCommand(resourceId, start, end));

        assertThat(view.userId()).isEqualTo(userId);
        assertThat(view.resourceId()).isEqualTo(resourceId);
        assertThat(view.status()).isEqualTo(BookingStatus.DRAFT);

        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void getById_throws_accessDenied_for_other_users_booking() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(me);

        AppUserEntity otherUser = new AppUserEntity(other, "o@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        OffsetDateTime start = OffsetDateTime.parse("2026-02-10T10:00:00Z");
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(bookingId, otherUser, resource, start, end, BookingStatus.DRAFT);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getById(bookingId))
                .isInstanceOf(com.ramil.booking.resource_booking.domain.booking.exception.BookingAccessDeniedException.class);

        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void cancel_throws_if_confirmed() {
        UUID me = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(me);

        AppUserEntity user = new AppUserEntity(me, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        OffsetDateTime start = OffsetDateTime.parse("2026-02-10T10:00:00Z");
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(bookingId, user, resource, start, end, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(bookingId))
                .isInstanceOf(BookingStatusException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmAfterPayment_throws_for_non_admin() {
        UUID me = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(currentUser.isAdmin()).thenReturn(false);

        AppUserEntity user = new AppUserEntity(me, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        OffsetDateTime start = OffsetDateTime.parse("2026-02-10T10:00:00Z");
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(bookingId, user, resource, start, end, BookingStatus.WAITING_PAYMENT);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmAfterPayment(bookingId))
                .isInstanceOf(com.ramil.booking.resource_booking.domain.booking.exception.BookingAccessDeniedException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }


}

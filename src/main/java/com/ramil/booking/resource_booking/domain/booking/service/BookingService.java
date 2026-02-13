package com.ramil.booking.resource_booking.domain.booking.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramil.booking.resource_booking.domain.booking.dto.BookingView;
import com.ramil.booking.resource_booking.domain.booking.dto.CreateBookingCommand;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingAccessDeniedException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingConflictException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingNotFoundException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingStatusException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingTimeRangeException;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceInactiveException;
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceNotFoundException;
import com.ramil.booking.resource_booking.domain.resource.repository.ResourceRepository;
import com.ramil.booking.resource_booking.domain.user.entity.AppUserEntity;
import com.ramil.booking.resource_booking.domain.user.repository.AppUserRepository;
import com.ramil.booking.resource_booking.domain.user.security.CurrentUserProvider;

@Service
public class BookingService {

    private static final List<BookingStatus> ACTIVE_FOR_CONFLICT = List.of(
            BookingStatus.WAITING_PAYMENT,
            BookingStatus.CONFIRMED
    );

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUser;

    public BookingService(
            BookingRepository bookingRepository,
            ResourceRepository resourceRepository,
            AppUserRepository appUserRepository,
            CurrentUserProvider currentUser
    ) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.resourceRepository = Objects.requireNonNull(resourceRepository);
        this.appUserRepository = Objects.requireNonNull(appUserRepository);
        this.currentUser = Objects.requireNonNull(currentUser);
    }

    @Transactional
    public BookingView createDraft(CreateBookingCommand cmd) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cmd.resourceId(), "resourceId");
        Objects.requireNonNull(cmd.startTime(), "startTime");
        Objects.requireNonNull(cmd.endTime(), "endTime");

        validateTimeRange(cmd.startTime(), cmd.endTime());

        UUID userId = currentUser.currentUserId();

        // лочим ресурс (опционально, но полезно)
        ResourceEntity resource = resourceRepository.findByIdForUpdate(cmd.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException(cmd.resourceId()));

        if (!resource.isActive()) {
            throw new ResourceInactiveException(resource.getId());
        }

        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: " + userId));

        // быстрая проверка (не железная, железная — constraint)
        boolean hasConflicts = !bookingRepository.findConflicts(
                resource.getId(), cmd.startTime(), cmd.endTime(), ACTIVE_FOR_CONFLICT
        ).isEmpty();

        if (hasConflicts) {
            throw new BookingConflictException(resource.getId(), cmd.startTime(), cmd.endTime());
        }

        BookingEntity booking = new BookingEntity(
                UUID.randomUUID(),
                user,
                resource,
                cmd.startTime(),
                cmd.endTime(),
                BookingStatus.DRAFT
        );

        return toView(bookingRepository.save(booking));
    }

    @Transactional
    public BookingView cancel(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        assertCanManage(booking);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BookingStatusException(bookingId, booking.getStatus(), "not CONFIRMED");
        }
        if (booking.getStatus() == BookingStatus.CANCELED) {
            return toView(booking);
        }

        booking.setStatus(BookingStatus.CANCELED);
        return toView(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingView getById(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        assertCanView(booking);
        return toView(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingView> listMyBookings() {
        UUID userId = currentUser.currentUserId();
        return bookingRepository.findByUserId(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingView> listAll() {
        if (!currentUser.isAdmin()) {
            throw new BookingAccessDeniedException(null);
        }
        return bookingRepository.findAllOrderByStartTimeDesc().stream().map(this::toView).toList();
    }

    @Transactional
    public BookingView markWaitingPayment(UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        assertCanManage(booking);

        if (booking.getStatus() != BookingStatus.DRAFT) {
            throw new BookingStatusException(bookingId, booking.getStatus(), "DRAFT");
        }

        booking.setStatus(BookingStatus.WAITING_PAYMENT);

        try {
            return toView(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException e) {
            throw new BookingConflictException(
                    booking.getResource().getId(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );
        }
    }

    @Transactional
    public BookingView confirmAfterPayment(UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!currentUser.isAdmin()) {
            throw new BookingAccessDeniedException(bookingId);
        }

        if (booking.getStatus() != BookingStatus.WAITING_PAYMENT) {
            throw new BookingStatusException(bookingId, booking.getStatus(), "WAITING_PAYMENT");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        try {
            return toView(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException e) {
            throw new BookingConflictException(
                    booking.getResource().getId(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );
        }
    }

    private void assertCanView(BookingEntity booking) {
        if (currentUser.isAdmin()) return;

        UUID me = currentUser.currentUserId();
        if (!booking.getUser().getId().equals(me)) {
            throw new BookingAccessDeniedException(booking.getId());
        }
    }

    private void assertCanManage(BookingEntity booking) {
        assertCanView(booking);
    }

    private static void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
        if (!end.isAfter(start)) {
            throw new BookingTimeRangeException(start, end);
        }
    }

    private BookingView toView(BookingEntity b) {
        return new BookingView(
                b.getId(),
                b.getUser().getId(),
                b.getResource().getId(),
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus()
        );
    }
}

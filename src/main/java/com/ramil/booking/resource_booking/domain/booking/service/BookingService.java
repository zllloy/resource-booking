package com.ramil.booking.resource_booking.domain.booking.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramil.booking.resource_booking.domain.booking.dto.BookingView;
import com.ramil.booking.resource_booking.domain.booking.dto.CreateBookingCommand;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingConflictException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingTimeRangeException;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceInactiveException;
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceNotFoundException;
import com.ramil.booking.resource_booking.domain.resource.repository.ResourceRepository;
import com.ramil.booking.resource_booking.domain.user.entity.AppUserEntity;
import com.ramil.booking.resource_booking.domain.user.repository.AppUserRepository;


@Service
public class BookingService {

    private static final List<BookingStatus> ACTIVE_FOR_CONFLICT = List.of(
            BookingStatus.WAITING_PAYMENT,
            BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final AppUserRepository appUserRepository;

    public BookingService(BookingRepository bookingRepository,
                          ResourceRepository resourceRepository,
                          AppUserRepository appUserRepository) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.resourceRepository = Objects.requireNonNull(resourceRepository);
        this.appUserRepository = Objects.requireNonNull(appUserRepository);
    }

    @Transactional
    public BookingView createDraft(CreateBookingCommand cmd) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cmd.userId(), "userId");
        Objects.requireNonNull(cmd.resourceId(), "resourceId");
        Objects.requireNonNull(cmd.startTime(), "startTime");
        Objects.requireNonNull(cmd.endTime(), "endTime");

        validateTimeRange(cmd.startTime(), cmd.endTime());

        ResourceEntity resource = resourceRepository.findById(cmd.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException(cmd.resourceId()));

        if (!resource.isActive()) {
            throw new ResourceInactiveException(resource.getId());
        }

        AppUserEntity user = appUserRepository.findById(cmd.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + cmd.userId()));

        boolean hasConflicts = !bookingRepository.findConflicts(
                resource.getId(),
                cmd.startTime(),
                cmd.endTime(),
                ACTIVE_FOR_CONFLICT).isEmpty();

        if (hasConflicts) {
            throw new BookingConflictException(resource.getId(), cmd.startTime(), cmd.endTime());
        }

        BookingEntity booking = new BookingEntity(
                UUID.randomUUID(),
                user,
                resource,
                cmd.startTime(),
                cmd.endTime(),
                BookingStatus.DRAFT);

        BookingEntity saved = bookingRepository.save(booking);
        return toView(saved);
    }

    @Transactional
    public BookingView cancel(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        // Разрешаем отмену только пока не подтверждено
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel CONFIRMED booking: " + bookingId);
        }

        booking.setStatus(BookingStatus.CANCELED);
        return toView(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingView getById(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toView(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingView> listForUser(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        return bookingRepository.findByUserId(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingView> listAll() {
        return bookingRepository.findAll().stream().map(this::toView).toList();
    }

    // На будущее для платежей:
    @Transactional
    public BookingView markWaitingPayment(UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.DRAFT) {
            throw new IllegalStateException("Booking must be DRAFT to start payment: " + bookingId);
        }

        booking.setStatus(BookingStatus.WAITING_PAYMENT);
        return toView(bookingRepository.save(booking));
    }

    @Transactional
    public BookingView confirmAfterPayment(UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.WAITING_PAYMENT) {
            throw new IllegalStateException("Booking must be WAITING_PAYMENT to confirm: " + bookingId);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return toView(bookingRepository.save(booking));
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
                b.getStatus());
    }
}
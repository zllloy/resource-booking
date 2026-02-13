package com.ramil.booking.resource_booking.domain.booking.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

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

        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("booking.createDraft requestedBy={} admin={} resourceId={} start={} end={}",
                me, admin, cmd.resourceId(), cmd.startTime(), cmd.endTime());

        validateTimeRange(cmd.startTime(), cmd.endTime());

        // лочим ресурс (опционально, но полезно)
        ResourceEntity resource = resourceRepository.findByIdForUpdate(cmd.resourceId())
                .orElseThrow(() -> {
                    log.warn("booking.createDraft resourceNotFound requestedBy={} resourceId={}", me, cmd.resourceId());
                    return new ResourceNotFoundException(cmd.resourceId());
                });

        if (!resource.isActive()) {
            log.warn("booking.createDraft resourceInactive requestedBy={} resourceId={}", me, resource.getId());
            throw new ResourceInactiveException(resource.getId());
        }

        AppUserEntity user = appUserRepository.findById(me)
                .orElseThrow(() -> {
                    log.error("booking.createDraft authenticatedUserMissingInDb userId={}", me);
                    return new IllegalStateException("Authenticated user not found in DB: " + me);
                });

        boolean hasConflicts = !bookingRepository.findConflicts(
                resource.getId(), cmd.startTime(), cmd.endTime(), ACTIVE_FOR_CONFLICT
        ).isEmpty();

        if (hasConflicts) {
            log.warn("booking.createDraft conflict requestedBy={} resourceId={} start={} end={}",
                    me, resource.getId(), cmd.startTime(), cmd.endTime());
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

        BookingEntity saved = bookingRepository.save(booking);

        log.info("booking.createDraft created requestedBy={} bookingId={} status={}",
                me, saved.getId(), saved.getStatus());

        return toView(saved);
    }

    @Transactional
    public BookingView cancel(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("booking.cancel requestedBy={} admin={} bookingId={}", me, admin, bookingId);

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("booking.cancel bookingNotFound requestedBy={} bookingId={}", me, bookingId);
                    return new BookingNotFoundException(bookingId);
                });

        assertCanManage(booking);

        BookingStatus current = booking.getStatus();

        if (current == BookingStatus.CONFIRMED) {
            log.warn("booking.cancel invalidStatus requestedBy={} bookingId={} current={}", me, bookingId, current);
            throw new BookingStatusException(bookingId, current, "not CONFIRMED");
        }
        if (current == BookingStatus.CANCELED) {
            log.info("booking.cancel alreadyCanceled requestedBy={} bookingId={}", me, bookingId);
            return toView(booking);
        }

        booking.setStatus(BookingStatus.CANCELED);
        BookingEntity saved = bookingRepository.save(booking);

        log.info("booking.cancel done requestedBy={} bookingId={} fromStatus={} toStatus={}",
                me, bookingId, current, saved.getStatus());

        return toView(saved);
    }

    @Transactional(readOnly = true)
    public BookingView getById(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("booking.getById requestedBy={} admin={} bookingId={}", me, admin, bookingId);

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("booking.getById bookingNotFound requestedBy={} bookingId={}", me, bookingId);
                    return new BookingNotFoundException(bookingId);
                });

        assertCanView(booking);

        log.info("booking.getById ok requestedBy={} bookingId={} status={}", me, bookingId, booking.getStatus());
        return toView(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingView> listMyBookings() {
        UUID me = currentUser.currentUserId();
        log.info("booking.listMyBookings requestedBy={}", me);

        List<BookingView> result = bookingRepository.findByUserId(me).stream().map(this::toView).toList();

        log.info("booking.listMyBookings result requestedBy={} count={}", me, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<BookingView> listAll() {
        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("booking.listAll requestedBy={} admin={}", me, admin);

        if (!admin) {
            log.warn("booking.listAll accessDenied requestedBy={}", me);
            throw new BookingAccessDeniedException(null);
        }

        List<BookingView> result = bookingRepository.findAllOrderByStartTimeDesc().stream().map(this::toView).toList();
        log.info("booking.listAll result requestedBy={} count={}", me, result.size());
        return result;
    }

    @Transactional
    public BookingView markWaitingPayment(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("booking.markWaitingPayment requestedBy={} admin={} bookingId={}", me, admin, bookingId);

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("booking.markWaitingPayment bookingNotFound requestedBy={} bookingId={}", me, bookingId);
                    return new BookingNotFoundException(bookingId);
                });

        assertCanManage(booking);

        BookingStatus current = booking.getStatus();
        if (current != BookingStatus.DRAFT) {
            log.warn("booking.markWaitingPayment invalidStatus requestedBy={} bookingId={} current={}",
                    me, bookingId, current);
            throw new BookingStatusException(bookingId, current, "DRAFT");
        }

        booking.setStatus(BookingStatus.WAITING_PAYMENT);

        try {
            BookingEntity saved = bookingRepository.saveAndFlush(booking);
            log.info("booking.markWaitingPayment done requestedBy={} bookingId={} fromStatus={} toStatus={}",
                    me, bookingId, current, saved.getStatus());
            return toView(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("booking.markWaitingPayment conflict requestedBy={} bookingId={} resourceId={} start={} end={}",
                    me, bookingId, booking.getResource().getId(), booking.getStartTime(), booking.getEndTime());
            throw new BookingConflictException(
                    booking.getResource().getId(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );
        }
    }

    @Transactional
    public BookingView confirmAfterPayment(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("booking.confirmAfterPayment requestedBy={} admin={} bookingId={}", me, admin, bookingId);

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("booking.confirmAfterPayment bookingNotFound requestedBy={} bookingId={}", me, bookingId);
                    return new BookingNotFoundException(bookingId);
                });

        if (!admin) {
            log.warn("booking.confirmAfterPayment accessDenied requestedBy={} bookingId={}", me, bookingId);
            throw new BookingAccessDeniedException(bookingId);
        }

        BookingStatus current = booking.getStatus();
        if (current != BookingStatus.WAITING_PAYMENT) {
            log.warn("booking.confirmAfterPayment invalidStatus requestedBy={} bookingId={} current={}",
                    me, bookingId, current);
            throw new BookingStatusException(bookingId, current, "WAITING_PAYMENT");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        try {
            BookingEntity saved = bookingRepository.saveAndFlush(booking);
            log.info("booking.confirmAfterPayment done requestedBy={} bookingId={} fromStatus={} toStatus={}",
                    me, bookingId, current, saved.getStatus());
            return toView(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("booking.confirmAfterPayment conflict requestedBy={} bookingId={} resourceId={} start={} end={}",
                    me, bookingId, booking.getResource().getId(), booking.getStartTime(), booking.getEndTime());
            throw new BookingConflictException(
                    booking.getResource().getId(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );
        }
    }

    private void assertCanView(BookingEntity booking) {
        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        if (admin) return;

        if (!booking.getUser().getId().equals(me)) {
            log.warn("booking.accessDenied requestedBy={} bookingId={} bookingUserId={}",
                    me, booking.getId(), booking.getUser().getId());
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

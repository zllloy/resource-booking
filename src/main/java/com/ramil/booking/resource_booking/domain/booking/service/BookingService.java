package com.ramil.booking.resource_booking.domain.booking.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingConflictException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingTimeRangeException;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.resource.dto.CreateBookingCommand;
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
    this.bookingRepository = bookingRepository;
    this.resourceRepository = resourceRepository;
    this.appUserRepository = appUserRepository;
  }

  @Transactional
  public UUID createDraft(CreateBookingCommand cmd) {
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

    UUID bookingId = UUID.randomUUID();
    BookingEntity booking = new BookingEntity(
        bookingId,
        user,
        resource,
        cmd.startTime(),
        cmd.endTime(),
        BookingStatus.DRAFT);

    bookingRepository.save(booking);
    return bookingId;
  }

  private static void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
    if (!end.isAfter(start)) {
      throw new BookingTimeRangeException(start, end);
    }
  }
}
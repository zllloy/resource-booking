package com.ramil.booking.resource_booking.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingNotFoundException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingStatusException;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.payment.dto.StartPaymentCommand;
import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;
import com.ramil.booking.resource_booking.domain.payment.exception.PaymentAccessDeniedException;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentStatus;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentType;
import com.ramil.booking.resource_booking.domain.payment.repository.PaymentRepository;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.user.entity.AppUserEntity;
import com.ramil.booking.resource_booking.domain.user.model.Role;
import com.ramil.booking.resource_booking.domain.user.security.CurrentUserProvider;

class PaymentTxServiceTest {

    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private CurrentUserProvider currentUser;

    private PaymentTxService paymentTxService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        currentUser = mock(CurrentUserProvider.class);
        paymentTxService = new PaymentTxService(paymentRepository, bookingRepository, currentUser);
    }

    @Test
    void startPaymentTx_creates_payment_when_booking_waiting_payment() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.WAITING_PAYMENT
        );

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId, PaymentProvider.CARD, PaymentType.INSTANT,
                new BigDecimal("100.00"), "USD", null
        );

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(userId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID paymentId = paymentTxService.startPaymentTx(cmd, null);

        assertThat(paymentId).isNotNull();
        verify(paymentRepository).save(argThat(p -> {
            assertThat(p.getBooking().getId()).isEqualTo(bookingId);
            assertThat(p.getProvider()).isEqualTo(PaymentProvider.CARD);
            assertThat(p.getType()).isEqualTo(PaymentType.INSTANT);
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.NEW);
            assertThat(p.getAmount()).isEqualByComparingTo("100.00");
            assertThat(p.getCurrency()).isEqualTo("USD");
            return true;
        }));
    }

    @Test
    void startPaymentTx_throws_when_booking_not_found() {
        UUID bookingId = UUID.randomUUID();
        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId, PaymentProvider.CARD, PaymentType.INSTANT,
                new BigDecimal("100.00"), "USD", null
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentTxService.startPaymentTx(cmd, null))
                .isInstanceOf(BookingNotFoundException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void startPaymentTx_throws_when_user_not_owner_and_not_admin() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity owner = new AppUserEntity(ownerId, "owner@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, owner, resource, start, end, BookingStatus.WAITING_PAYMENT
        );

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId, PaymentProvider.CARD, PaymentType.INSTANT,
                new BigDecimal("100.00"), "USD", null
        );

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(otherUserId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentTxService.startPaymentTx(cmd, null))
                .isInstanceOf(PaymentAccessDeniedException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void startPaymentTx_allows_admin_to_pay_any_booking() {
        UUID ownerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity owner = new AppUserEntity(ownerId, "owner@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, owner, resource, start, end, BookingStatus.WAITING_PAYMENT
        );

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId, PaymentProvider.CARD, PaymentType.INSTANT,
                new BigDecimal("100.00"), "USD", null
        );

        when(currentUser.isAdmin()).thenReturn(true);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID paymentId = paymentTxService.startPaymentTx(cmd, null);

        assertThat(paymentId).isNotNull();
        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void startPaymentTx_throws_when_booking_not_waiting_payment() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.DRAFT
        );

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId, PaymentProvider.CARD, PaymentType.INSTANT,
                new BigDecimal("100.00"), "USD", null
        );

        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentUserId()).thenReturn(userId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentTxService.startPaymentTx(cmd, null))
                .isInstanceOf(BookingStatusException.class)
                .hasMessageContaining("WAITING_PAYMENT");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void finalizePaymentTx_success_confirms_booking_and_sets_paid_fields() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity user = new AppUserEntity(userId, "user@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.WAITING_PAYMENT
        );
        assertThat(booking.getPaidAt()).isNull();
        assertThat(booking.getPaidBy()).isNull();

        PaymentEntity payment = new PaymentEntity(
                paymentId, booking, PaymentProvider.CARD, PaymentType.INSTANT,
                PaymentStatus.NEW, new BigDecimal("100.00"), "USD", null
        );

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(currentUser.currentUserEmail()).thenReturn("user@test.com");
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentEntity result = paymentTxService.finalizePaymentTx(paymentId, true);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaidAt()).isNotNull();
        assertThat(booking.getPaidBy()).isEqualTo("user@test.com");
        verify(bookingRepository).save(booking);
    }

    @Test
    void finalizePaymentTx_failure_sets_payment_failed_and_cancels_booking() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.WAITING_PAYMENT
        );
        PaymentEntity payment = new PaymentEntity(
                paymentId, booking, PaymentProvider.CARD, PaymentType.INSTANT,
                PaymentStatus.NEW, new BigDecimal("100.00"), "USD", null
        );

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentEntity result = paymentTxService.finalizePaymentTx(paymentId, false);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void finalizePaymentTx_success_throws_when_booking_not_waiting_payment() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource, start, end, BookingStatus.CONFIRMED
        );
        PaymentEntity payment = new PaymentEntity(
                paymentId, booking, PaymentProvider.CARD, PaymentType.INSTANT,
                PaymentStatus.NEW, new BigDecimal("100.00"), "USD", null
        );

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentTxService.finalizePaymentTx(paymentId, true))
                .isInstanceOf(BookingStatusException.class)
                .hasMessageContaining("WAITING_PAYMENT");

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void finalizePaymentTx_throws_when_payment_not_found() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentTxService.finalizePaymentTx(paymentId, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }
}

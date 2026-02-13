package com.ramil.booking.resource_booking.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.payment.dto.PaymentView;
import com.ramil.booking.resource_booking.domain.payment.dto.StartPaymentCommand;
import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentStatus;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentType;
import com.ramil.booking.resource_booking.domain.payment.provider.PaymentProviderClient;
import com.ramil.booking.resource_booking.domain.payment.repository.PaymentRepository;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.user.entity.AppUserEntity;
import com.ramil.booking.resource_booking.domain.user.model.Role;
import com.ramil.booking.resource_booking.domain.user.security.CurrentUserProvider;
import com.ramil.booking.resource_booking.domain.payment.mapper.PaymentMapper;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentTxService paymentTxService;
    private PaymentProviderClient cardClient;
    private ObjectMapper objectMapper;

    private BookingRepository bookingRepository;
    private CurrentUserProvider currentUser;
    private PaymentMapper paymentMapper;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentTxService = mock(PaymentTxService.class);
        cardClient = mock(PaymentProviderClient.class);
        objectMapper = new ObjectMapper();

        bookingRepository = mock(BookingRepository.class);
        currentUser = mock(CurrentUserProvider.class);
        paymentMapper = mock(PaymentMapper.class);

        when(cardClient.provider()).thenReturn(PaymentProvider.CARD);

        paymentService = new PaymentService(
                paymentRepository,
                paymentTxService,
                List.of(cardClient),
                objectMapper,
                bookingRepository,
                currentUser,
                paymentMapper
        );
    }

    @Test
    void startPayment_success_when_provider_ok() {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId,
                PaymentProvider.CARD,
                PaymentType.INSTANT,
                new BigDecimal("100.00"),
                "USD",
                "{\"test\":true}"
        );

        when(paymentTxService.startPaymentTx(eq(cmd), any())).thenReturn(paymentId);
        when(cardClient.charge(eq(cmd.amount()), eq(cmd.currency()), eq(cmd.payloadJson()))).thenReturn(true);

        PaymentEntity finalized = paymentEntity(paymentId, bookingId, PaymentStatus.SUCCESS, cmd);
        when(paymentTxService.finalizePaymentTx(paymentId, true)).thenReturn(finalized);
        PaymentView expectedView = new PaymentView(paymentId, bookingId, cmd.provider(), cmd.type(), PaymentStatus.SUCCESS, cmd.amount(), cmd.currency());
        when(paymentMapper.toView(finalized)).thenReturn(expectedView);

        PaymentView view = paymentService.startPayment(cmd);

        assertThat(view.id()).isEqualTo(paymentId);
        assertThat(view.bookingId()).isEqualTo(bookingId);
        assertThat(view.status()).isEqualTo(PaymentStatus.SUCCESS);

        verify(paymentTxService).startPaymentTx(eq(cmd), any());
        verify(cardClient).charge(eq(cmd.amount()), eq(cmd.currency()), eq(cmd.payloadJson()));
        verify(paymentTxService).finalizePaymentTx(paymentId, true);
    }

    @Test
    void startPayment_failed_when_provider_returns_false() {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId,
                PaymentProvider.CARD,
                PaymentType.INSTANT,
                new BigDecimal("100.00"),
                "USD",
                "{\"forceFail\":true}"
        );

        when(paymentTxService.startPaymentTx(eq(cmd), any())).thenReturn(paymentId);
        when(cardClient.charge(eq(cmd.amount()), eq(cmd.currency()), eq(cmd.payloadJson()))).thenReturn(false);

        PaymentEntity finalized = paymentEntity(paymentId, bookingId, PaymentStatus.FAILED, cmd);
        when(paymentTxService.finalizePaymentTx(paymentId, false)).thenReturn(finalized);
        PaymentView expectedView = new PaymentView(paymentId, bookingId, cmd.provider(), cmd.type(), PaymentStatus.FAILED, cmd.amount(), cmd.currency());
        when(paymentMapper.toView(finalized)).thenReturn(expectedView);

        PaymentView view = paymentService.startPayment(cmd);

        assertThat(view.status()).isEqualTo(PaymentStatus.FAILED);

        verify(paymentTxService).finalizePaymentTx(paymentId, false);
    }

    @Test
    void startPayment_throws_if_unknown_provider() {
        UUID bookingId = UUID.randomUUID();

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId,
                PaymentProvider.PAYPAL,
                PaymentType.INSTANT,
                new BigDecimal("100.00"),
                "USD",
                "{}"
        );

        assertThatThrownBy(() -> paymentService.startPayment(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No client for provider");

        verifyNoInteractions(paymentTxService);
    }

    @Test
    void startPayment_throws_if_payloadJson_invalid() {
        UUID bookingId = UUID.randomUUID();

        StartPaymentCommand cmd = new StartPaymentCommand(
                bookingId,
                PaymentProvider.CARD,
                PaymentType.INSTANT,
                new BigDecimal("100.00"),
                "USD",
                "{not-a-json"
        );

        assertThatThrownBy(() -> paymentService.startPayment(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid payloadJson");

        verifyNoInteractions(paymentTxService);
        verify(cardClient, never()).charge(any(), any(), any()); // важно: именно charge не должен вызваться
    }

    private static PaymentEntity paymentEntity(UUID paymentId, UUID bookingId, PaymentStatus status, StartPaymentCommand cmd) {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AppUserEntity user = new AppUserEntity(userId, "u@test.com", "hash", Role.USER);
        ResourceEntity resource = new ResourceEntity(resourceId, "Room 1", null, true);

        BookingEntity booking = new BookingEntity(
                bookingId, user, resource,
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now().plusHours(1),
                BookingStatus.DRAFT
        );

        return new PaymentEntity(
                paymentId,
                booking,
                cmd.provider(),
                cmd.type(),
                status,
                cmd.amount(),
                cmd.currency(),
                null
        );
    }
}

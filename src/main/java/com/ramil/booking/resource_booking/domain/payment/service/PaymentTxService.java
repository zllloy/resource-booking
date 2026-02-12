package com.ramil.booking.resource_booking.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.booking.service.BookingService;
import com.ramil.booking.resource_booking.domain.payment.dto.StartPaymentCommand;
import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentStatus;
import com.ramil.booking.resource_booking.domain.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentTxService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTxService.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public PaymentTxService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BookingService bookingService
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.bookingService = Objects.requireNonNull(bookingService);
    }

    /**
     * TX#1: создаём payment + переводим booking в WAITING_PAYMENT
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID startPaymentTx(StartPaymentCommand cmd, JsonNode payload) {
        BookingEntity booking = bookingRepository.findById(cmd.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + cmd.bookingId()));

        if (booking.getStatus() != BookingStatus.DRAFT) {
            throw new IllegalStateException("Booking must be DRAFT to start payment: " + booking.getId());
        }

        PaymentEntity payment = new PaymentEntity(
                UUID.randomUUID(),
                booking,
                cmd.provider(),
                cmd.type(),
                PaymentStatus.NEW,
                cmd.amount(),
                cmd.currency(),
                payload
        );

        paymentRepository.save(payment);

        // DRAFT -> WAITING_PAYMENT
        bookingService.markWaitingPayment(booking.getId());

        log.info("Payment initiated: paymentId={}, bookingId={}, provider={}, type={}",
                payment.getId(), booking.getId(), cmd.provider(), cmd.type());

        return payment.getId();
    }

    /**
     * TX#2: проставить payment SUCCESS/FAILED + booking CONFIRMED/CANCELED
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentEntity finalizePaymentTx(UUID paymentId, boolean ok) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        UUID bookingId = payment.getBooking().getId();

        if (ok) {
            payment.setStatus(PaymentStatus.SUCCESS);
            bookingService.confirmAfterPayment(bookingId);
            log.info("Payment success: paymentId={}, bookingId={}", paymentId, bookingId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            bookingService.cancel(bookingId);
            log.warn("Payment failed: paymentId={}, bookingId={}", paymentId, bookingId);
        }

        return paymentRepository.save(payment);
    }
}
package com.ramil.booking.resource_booking.domain.payment.service;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingNotFoundException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingStatusException;
import com.ramil.booking.resource_booking.domain.booking.model.BookingStatus;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.payment.dto.StartPaymentCommand;
import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentStatus;
import com.ramil.booking.resource_booking.domain.payment.repository.PaymentRepository;

@Service
public class PaymentTxService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentTxService(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
    }

    @Transactional
    public UUID startPaymentTx(StartPaymentCommand cmd, JsonNode payload) {
        BookingEntity booking = bookingRepository.findById(cmd.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(cmd.bookingId()));

        if (booking.getStatus() != BookingStatus.DRAFT) {
            throw new BookingStatusException(booking.getId(), booking.getStatus(), "DRAFT");
        }

        booking.setStatus(BookingStatus.WAITING_PAYMENT);
        bookingRepository.saveAndFlush(booking);

        UUID paymentId = UUID.randomUUID();
        PaymentEntity payment = new PaymentEntity(
                paymentId,
                booking,
                cmd.provider(),
                cmd.type(),
                PaymentStatus.NEW,
                cmd.amount(),
                cmd.currency(),
                payload
        );

        paymentRepository.save(payment);
        return paymentId;
    }

    @Transactional
    public PaymentEntity finalizePaymentTx(UUID paymentId, boolean ok) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        BookingEntity booking = payment.getBooking();

        if (ok) {
            payment.setStatus(PaymentStatus.SUCCESS);

            // подтверждение ТОЛЬКО из WAITING_PAYMENT
            if (booking.getStatus() != BookingStatus.WAITING_PAYMENT) {
                throw new BookingStatusException(booking.getId(), booking.getStatus(), "WAITING_PAYMENT");
            }
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        } else {
            payment.setStatus(PaymentStatus.FAILED);

            if (booking.getStatus() == BookingStatus.WAITING_PAYMENT) {
                booking.setStatus(BookingStatus.CANCELED);
                bookingRepository.save(booking);
            }
        }

        return paymentRepository.save(payment);
    }
}

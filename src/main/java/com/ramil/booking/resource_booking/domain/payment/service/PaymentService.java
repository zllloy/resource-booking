package com.ramil.booking.resource_booking.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingNotFoundException;
import com.ramil.booking.resource_booking.domain.booking.repository.BookingRepository;
import com.ramil.booking.resource_booking.domain.payment.dto.PaymentView;
import com.ramil.booking.resource_booking.domain.payment.dto.StartPaymentCommand;
import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;
import com.ramil.booking.resource_booking.domain.payment.mapper.PaymentMapper;
import com.ramil.booking.resource_booking.domain.payment.exception.PaymentAccessDeniedException;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.provider.PaymentProviderClient;
import com.ramil.booking.resource_booking.domain.payment.repository.PaymentRepository;
import com.ramil.booking.resource_booking.domain.user.security.CurrentUserProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// Сервис для обработки платежей через различные провайдеры
// После успешной оплаты автоматически подтверждает бронирование
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentTxService paymentTxService;
    private final Map<PaymentProvider, PaymentProviderClient> clients;
    private final ObjectMapper objectMapper;

    private final BookingRepository bookingRepository;
    private final CurrentUserProvider currentUser;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentTxService paymentTxService,
            List<PaymentProviderClient> clientList,
            ObjectMapper objectMapper,
            BookingRepository bookingRepository,
            CurrentUserProvider currentUser,
            PaymentMapper paymentMapper
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.paymentTxService = Objects.requireNonNull(paymentTxService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.paymentMapper = Objects.requireNonNull(paymentMapper);

        this.clients = clientList.stream().collect(Collectors.toMap(
                PaymentProviderClient::provider,
                c -> c,
                (a, b) -> a
        ));
    }

    // Запускает процесс оплаты бронирования
    // После успешной оплаты бронирование автоматически подтверждается
    public PaymentView startPayment(StartPaymentCommand cmd) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cmd.bookingId(), "bookingId");
        Objects.requireNonNull(cmd.provider(), "provider");
        Objects.requireNonNull(cmd.type(), "type");
        Objects.requireNonNull(cmd.amount(), "amount");
        Objects.requireNonNull(cmd.currency(), "currency");

        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("payment.start requestedBy={} admin={} bookingId={} provider={} type={} amount={} currency={} payloadPresent={}",
                me, admin, cmd.bookingId(), cmd.provider(), cmd.type(), cmd.amount(), cmd.currency(),
                cmd.payloadJson() != null && !cmd.payloadJson().isBlank()
        );

        PaymentProviderClient client = clients.get(cmd.provider());
        if (client == null) {
            log.warn("payment.start unknownProvider requestedBy={} bookingId={} provider={}",
                    me, cmd.bookingId(), cmd.provider());
            throw new IllegalArgumentException("No client for provider: " + cmd.provider());
        }

        JsonNode payload = parsePayload(cmd.payloadJson());

        UUID paymentId = paymentTxService.startPaymentTx(cmd, payload);
        log.info("payment.tx.started requestedBy={} bookingId={} paymentId={}",
                me, cmd.bookingId(), paymentId);

        boolean ok;
        try {
            ok = client.charge(cmd.amount(), cmd.currency(), cmd.payloadJson());
        } catch (RuntimeException e) {
            // если провайдер упал — логируем как error и пробрасываем дальше
            log.error("payment.charge.error requestedBy={} bookingId={} paymentId={} provider={}",
                    me, cmd.bookingId(), paymentId, cmd.provider(), e);
            throw e;
        }

        log.info("payment.charge.result requestedBy={} bookingId={} paymentId={} ok={}",
                me, cmd.bookingId(), paymentId, ok);

        PaymentEntity finalized = paymentTxService.finalizePaymentTx(paymentId, ok);

        log.info("payment.finalized requestedBy={} bookingId={} paymentId={} status={}",
                me, finalized.getBooking().getId(), finalized.getId(), finalized.getStatus());

        return toView(finalized);
    }

    private JsonNode parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return null;
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException e) {
            log.warn("payment.payload.invalid payloadLength={}", payloadJson.length());
            throw new IllegalArgumentException("Invalid payloadJson", e);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentView> listByBooking(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");

        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("payment.listByBooking requestedBy={} admin={} bookingId={}", me, admin, bookingId);

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("payment.listByBooking bookingNotFound requestedBy={} bookingId={}", me, bookingId);
                    return new BookingNotFoundException(bookingId);
                });

        if (!admin && !booking.getUser().getId().equals(me)) {
            log.warn("payment.listByBooking accessDenied requestedBy={} bookingId={} bookingUserId={}",
                    me, bookingId, booking.getUser().getId());
            throw new PaymentAccessDeniedException(bookingId);
        }

        List<PaymentView> result = paymentRepository.findByBooking_Id(bookingId).stream()
                .map(this::toView)
                .toList();

        log.info("payment.listByBooking result requestedBy={} bookingId={} count={}", me, bookingId, result.size());
        return result;
    }

    private PaymentView toView(PaymentEntity p) {
        return paymentMapper.toView(p);
    }

    @Transactional(readOnly = true)
    public List<PaymentView> listMyPayments() {
        UUID me = currentUser.currentUserId();
        log.info("payment.listMyPayments requestedBy={}", me);

        List<PaymentView> result = paymentRepository.findByBooking_User_Id(me).stream()
                .map(this::toView)
                .toList();

        log.info("payment.listMyPayments result requestedBy={} count={}", me, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<PaymentView> listAllPayments() {
        UUID me = currentUser.currentUserId();
        boolean admin = currentUser.isAdmin();

        log.info("payment.listAllPayments requestedBy={} admin={}", me, admin);

        if (!admin) {
            log.warn("payment.listAllPayments accessDenied requestedBy={}", me);
            throw new PaymentAccessDeniedException(null);
        }

        List<PaymentView> result = paymentRepository.findAll().stream()
                .map(this::toView)
                .toList();

        log.info("payment.listAllPayments result requestedBy={} count={}", me, result.size());
        return result;
    }
}

package com.ramil.booking.resource_booking.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramil.booking.resource_booking.domain.payment.dto.PaymentView;
import com.ramil.booking.resource_booking.domain.payment.dto.StartPaymentCommand;
import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;
import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import com.ramil.booking.resource_booking.domain.payment.provider.PaymentProviderClient;
import com.ramil.booking.resource_booking.domain.payment.repository.PaymentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentTxService paymentTxService;
    private final Map<PaymentProvider, PaymentProviderClient> clients;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentTxService paymentTxService,
            List<PaymentProviderClient> clientList,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.paymentTxService = Objects.requireNonNull(paymentTxService);

        this.clients = clientList.stream().collect(Collectors.toMap(
                PaymentProviderClient::provider,
                c -> c,
                (a, b) -> a
        ));

        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public PaymentView startPayment(StartPaymentCommand cmd) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cmd.bookingId(), "bookingId");
        Objects.requireNonNull(cmd.provider(), "provider");
        Objects.requireNonNull(cmd.type(), "type");
        Objects.requireNonNull(cmd.amount(), "amount");
        Objects.requireNonNull(cmd.currency(), "currency");

        PaymentProviderClient client = clients.get(cmd.provider());
        if (client == null) {
            throw new IllegalArgumentException("No client for provider: " + cmd.provider());
        }

        JsonNode payload = parsePayload(cmd.payloadJson());

        UUID paymentId = paymentTxService.startPaymentTx(cmd, payload);

        boolean ok = client.charge(cmd.amount(), cmd.currency(), cmd.payloadJson());

        PaymentEntity finalized = paymentTxService.finalizePaymentTx(paymentId, ok);

        return toView(finalized);
    }

    private JsonNode parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return null;
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid payloadJson", e);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentView> listByBooking(UUID bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");
        return paymentRepository.findByBooking_Id(bookingId).stream().map(this::toView).toList();
    }

    private PaymentView toView(PaymentEntity p) {
        return new PaymentView(
                p.getId(),
                p.getBooking().getId(),
                p.getProvider(),
                p.getType(),
                p.getStatus(),
                p.getAmount(),
                p.getCurrency()
        );
    }
}

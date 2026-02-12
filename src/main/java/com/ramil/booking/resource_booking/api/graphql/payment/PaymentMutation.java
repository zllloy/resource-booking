package com.ramil.booking.resource_booking.api.graphql.payment;

import com.ramil.booking.resource_booking.domain.payment.dto.PaymentView;
import com.ramil.booking.resource_booking.domain.payment.dto.StartPaymentCommand;
import com.ramil.booking.resource_booking.domain.payment.service.PaymentService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class PaymentMutation {

    private final PaymentService paymentService;

    public PaymentMutation(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @MutationMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public PaymentView startPayment(@Argument StartPaymentInput input) {
        return paymentService.startPayment(new StartPaymentCommand(
                UUID.fromString(input.bookingId()),
                input.provider(),
                input.type(),
                new BigDecimal(input.amount()),
                input.currency(),
                input.payloadJson()
        ));
    }

    public record StartPaymentInput(
            String bookingId,
            com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider provider,
            com.ramil.booking.resource_booking.domain.payment.model.PaymentType type,
            String amount,
            String currency,
            String payloadJson
    ) {}
}


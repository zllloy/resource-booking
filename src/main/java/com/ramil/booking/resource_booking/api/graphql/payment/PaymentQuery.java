package com.ramil.booking.resource_booking.api.graphql.payment;

import com.ramil.booking.resource_booking.domain.payment.dto.PaymentView;
import com.ramil.booking.resource_booking.domain.payment.service.PaymentService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class PaymentQuery {

    private final PaymentService paymentService;

    public PaymentQuery(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public List<PaymentView> paymentsByBooking(@Argument UUID bookingId) {
        return paymentService.listByBooking(bookingId);
    }
}

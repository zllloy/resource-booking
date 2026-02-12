package com.ramil.booking.resource_booking.domain.payment.provider;

import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;

import java.math.BigDecimal;

public interface PaymentProviderClient {
    PaymentProvider provider();

    boolean charge(BigDecimal amount, String currency, String payloadJson);

    boolean cancel(String payloadJson);
}

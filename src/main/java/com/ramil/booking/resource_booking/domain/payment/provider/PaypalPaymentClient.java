package com.ramil.booking.resource_booking.domain.payment.provider;

import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaypalPaymentClient implements PaymentProviderClient {
    @Override public PaymentProvider provider() { return PaymentProvider.PAYPAL; }

    @Override
    public boolean charge(BigDecimal amount, String currency, String payloadJson) {
        return payloadJson == null || !payloadJson.contains("fail");
    }

    @Override
    public boolean cancel(String payloadJson) { return true; }
}

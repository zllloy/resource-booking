package com.ramil.booking.resource_booking.domain.payment.provider;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.ramil.booking.resource_booking.domain.payment.model.PaymentProvider;

@Component
public class CardPaymentClient implements PaymentProviderClient {

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.CARD;
    }

    @Override
    public boolean charge(BigDecimal amount, String currency, String payloadJson) {
        // для тестов: если payload содержит "forceFail":true -> fail
        return payloadJson == null || !payloadJson.contains("\"forceFail\":true");
    }

    @Override
    public boolean cancel(String payloadJson) {
        return true;
    }
}
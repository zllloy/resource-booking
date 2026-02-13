package com.ramil.booking.resource_booking.domain.payment.mapper;

import com.ramil.booking.resource_booking.domain.payment.dto.PaymentView;
import com.ramil.booking.resource_booking.domain.payment.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

// Маппер для преобразования PaymentEntity в PaymentView
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    // Извлекаем bookingId из вложенной сущности
    @Mapping(target = "bookingId", expression = "java(entity.getBooking() != null ? entity.getBooking().getId() : null)")
    PaymentView toView(PaymentEntity entity);
}

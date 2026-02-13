package com.ramil.booking.resource_booking.domain.booking.mapper;

import com.ramil.booking.resource_booking.domain.booking.dto.BookingView;
import com.ramil.booking.resource_booking.domain.booking.entity.BookingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

// Маппер для преобразования BookingEntity в BookingView
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingMapper {

    // Извлекаем userId и resourceId из вложенных сущностей
    @Mapping(target = "userId", expression = "java(entity.getUser() != null ? entity.getUser().getId() : null)")
    @Mapping(target = "resourceId", expression = "java(entity.getResource() != null ? entity.getResource().getId() : null)")
    BookingView toView(BookingEntity entity);
}

package com.ramil.booking.resource_booking.domain.resource.mapper;

import com.ramil.booking.resource_booking.domain.resource.dto.ResourceView;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

// Маппер для преобразования ResourceEntity в ResourceView
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ResourceMapper {

    ResourceView toView(ResourceEntity entity);
}

package com.ramil.booking.resource_booking.api.graphql.resource;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.ramil.booking.resource_booking.domain.booking.service.ResourceService;
import com.ramil.booking.resource_booking.domain.resource.dto.ResourceView;

@Controller
public class ResourceQuery {

  private final ResourceService resourceService;

  public ResourceQuery(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @QueryMapping
  public ResourceView resourceById(@Argument UUID id) {
    return resourceService.getById(id);
  }

  @QueryMapping
  public List<ResourceView> resources(@Argument Boolean active) {
    return resourceService.list(active);
  }
}
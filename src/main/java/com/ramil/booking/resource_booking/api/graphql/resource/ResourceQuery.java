package com.ramil.booking.resource_booking.api.graphql.resource;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.ramil.booking.resource_booking.domain.resource.dto.ResourceView;
import com.ramil.booking.resource_booking.domain.resource.service.ResourceService;

@Controller
public class ResourceQuery {

  private final ResourceService resourceService;

  public ResourceQuery(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResourceView resource(@Argument UUID id) {
    return resourceService.getById(id);
  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<ResourceView> resources(@Argument Boolean active) {
    return resourceService.list(active);
  }

}
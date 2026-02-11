package com.ramil.booking.resource_booking.api.graphql.resource;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.ramil.booking.resource_booking.domain.booking.service.ResourceService;
import com.ramil.booking.resource_booking.domain.resource.dto.CreateResourceCommand;
import com.ramil.booking.resource_booking.domain.resource.dto.ResourceView;
import com.ramil.booking.resource_booking.domain.resource.dto.UpdateResourceCommand;

@Controller
public class ResourceMutation {

  private final ResourceService resourceService;

  public ResourceMutation(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResourceView createResource(@Argument CreateResourceInput input) {
    return resourceService.create(new CreateResourceCommand(input.name(), input.description()));
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResourceView updateResource(@Argument UpdateResourceInput input) {
    return resourceService.update(new UpdateResourceCommand(input.id(), input.name(), input.description()));
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  public boolean deleteResource(@Argument UUID id) {
    resourceService.delete(id);
    return true;
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResourceView activateResource(@Argument UUID id) {
    return resourceService.activate(id);
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResourceView deactivateResource(@Argument UUID id) {
    return resourceService.deactivate(id);
  }

  public record CreateResourceInput(String name, String description) {}
  public record UpdateResourceInput(UUID id, String name, String description) {}
}
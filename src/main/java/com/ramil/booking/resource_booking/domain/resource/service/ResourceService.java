package com.ramil.booking.resource_booking.domain.resource.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ramil.booking.resource_booking.domain.resource.dto.CreateResourceCommand;
import com.ramil.booking.resource_booking.domain.resource.dto.ResourceView;
import com.ramil.booking.resource_booking.domain.resource.dto.UpdateResourceCommand;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.resource.repository.ResourceRepository;



@Service
@Transactional
public class ResourceService {

  private final ResourceRepository resourceRepository;

  public ResourceService(ResourceRepository resourceRepository) {
    this.resourceRepository = Objects.requireNonNull(resourceRepository);
  }

  public ResourceView create(CreateResourceCommand cmd) {
    Objects.requireNonNull(cmd);
    Objects.requireNonNull(cmd.name());

    UUID id = UUID.randomUUID();
    ResourceEntity entity = new ResourceEntity(id, cmd.name(), cmd.description(), true);
    ResourceEntity saved = resourceRepository.save(entity);
    return toView(saved);
  }

  public ResourceView update(UpdateResourceCommand cmd) {
    Objects.requireNonNull(cmd);
    Objects.requireNonNull(cmd.id());
    Objects.requireNonNull(cmd.name());

    ResourceEntity entity = resourceRepository.findById(cmd.id())
        .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + cmd.id()));

    entity.setName(cmd.name());
    entity.setDescription(cmd.description());

    return toView(entity);
  }

  public void delete(UUID id) {
    Objects.requireNonNull(id);
    resourceRepository.deleteById(id);
  }

  public ResourceView activate(UUID id) {
    Objects.requireNonNull(id);
    ResourceEntity entity = resourceRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));
    entity.setActive(true);
    return toView(entity);
  }

  public ResourceView deactivate(UUID id) {
    Objects.requireNonNull(id);
    ResourceEntity entity = resourceRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));
    entity.setActive(false);
    return toView(entity);
  }

  @Transactional(readOnly = true)
  public ResourceView getById(UUID id) {
    Objects.requireNonNull(id);
    return resourceRepository.findById(id)
        .map(this::toView)
        .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));
  }

  @Transactional(readOnly = true)
  public List<ResourceView> list() {
    return resourceRepository.findAll().stream().map(this::toView).toList();
  }

  private ResourceView toView(ResourceEntity e) {
    return new ResourceView(e.getId(), e.getName(), e.getDescription(), e.isActive());
  }
}
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
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceNotFoundException;
import com.ramil.booking.resource_booking.domain.resource.repository.ResourceRepository;

@Service
public class ResourceService {

  private final ResourceRepository resourceRepository;

  public ResourceService(ResourceRepository resourceRepository) {
    this.resourceRepository = Objects.requireNonNull(resourceRepository);
  }

  @Transactional
  public ResourceView create(CreateResourceCommand cmd) {
    Objects.requireNonNull(cmd, "cmd must not be null");

    ResourceEntity entity = new ResourceEntity(
        UUID.randomUUID(),
        cmd.name(),
        cmd.description(),
        true);

    ResourceEntity saved = resourceRepository.save(entity);
    return toView(saved);
  }

  @Transactional
  public ResourceView update(UpdateResourceCommand cmd) {
    Objects.requireNonNull(cmd, "cmd must not be null");

    ResourceEntity entity = resourceRepository.findById(cmd.id())
        .orElseThrow(() -> new ResourceNotFoundException(cmd.id()));

    entity.setName(cmd.name());
    entity.setDescription(cmd.description());

    ResourceEntity saved = resourceRepository.save(entity);
    return toView(saved);
  }

  @Transactional
  public void delete(UUID id) {
    Objects.requireNonNull(id, "id must not be null");

    if (!resourceRepository.existsById(id)) {
      throw new ResourceNotFoundException(id);
    }
    resourceRepository.deleteById(id);
  }

  @Transactional
  public ResourceView activate(UUID id) {
    Objects.requireNonNull(id, "id must not be null");

    ResourceEntity entity = resourceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(id));

    entity.setActive(true);
    return toView(resourceRepository.save(entity));
  }

  @Transactional
  public ResourceView deactivate(UUID id) {
    Objects.requireNonNull(id, "id must not be null");

    ResourceEntity entity = resourceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(id));

    entity.setActive(false);
    return toView(resourceRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public ResourceView getById(UUID id) {
    Objects.requireNonNull(id, "id must not be null");

    ResourceEntity entity = resourceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(id));

    return toView(entity);
  }

  @Transactional(readOnly = true)
  public List<ResourceView> list(Boolean active) {
    List<ResourceEntity> list = (active == null)
        ? resourceRepository.findAll()
        : resourceRepository.findByActive(active);

    return list.stream().map(this::toView).toList();
  }

  private ResourceView toView(ResourceEntity e) {
    return new ResourceView(e.getId(), e.getName(), e.getDescription(), e.isActive());
  }

}
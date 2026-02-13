package com.ramil.booking.resource_booking.domain.resource.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.ramil.booking.resource_booking.domain.resource.exception.InvalidResourceDescriptionException;
import com.ramil.booking.resource_booking.domain.resource.exception.InvalidResourceNameException;
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

    private static final int MAX_NAME_LEN = 100;
    private static final int MAX_DESC_LEN = 1000;

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = Objects.requireNonNull(resourceRepository);
    }

    @Transactional
    public ResourceView create(CreateResourceCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        String name = normalizeAndValidateName(cmd.name());
        String description = normalizeAndValidateDescription(cmd.description());

        ResourceEntity entity = new ResourceEntity(
                UUID.randomUUID(),
                name,
                description,
                true
        );

        ResourceEntity saved = resourceRepository.save(entity);
        return toView(saved);
    }

    @Transactional
    public ResourceView update(UpdateResourceCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");
        Objects.requireNonNull(cmd.id(), "id must not be null");

        ResourceEntity entity = resourceRepository.findById(cmd.id())
                .orElseThrow(() -> new ResourceNotFoundException(cmd.id()));
        String name = normalizeAndValidateName(cmd.name());
        String description = normalizeAndValidateDescription(cmd.description());

        entity.setName(name);
        entity.setDescription(description);

        return toView(entity);
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

        if (!entity.isActive()) {
            entity.setActive(true);
        }
        return toView(entity);
    }

    @Transactional
    public ResourceView deactivate(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        ResourceEntity entity = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));

        if (entity.isActive()) {
            entity.setActive(false);
        }
        return toView(entity);
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

    private String normalizeAndValidateName(String name) {
        if (name == null) throw InvalidResourceNameException.blank();

        String normalized = name.trim();
        if (normalized.isBlank()) throw InvalidResourceNameException.blank();
        if (normalized.length() > MAX_NAME_LEN) throw InvalidResourceNameException.tooLong(MAX_NAME_LEN);

        return normalized;
    }

    private String normalizeAndValidateDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        if (normalized.length() > MAX_DESC_LEN) throw InvalidResourceDescriptionException.tooLong(MAX_DESC_LEN);

        return normalized.isBlank() ? null : normalized;
    }

    private ResourceView toView(ResourceEntity e) {
        return new ResourceView(e.getId(), e.getName(), e.getDescription(), e.isActive());
    }
}
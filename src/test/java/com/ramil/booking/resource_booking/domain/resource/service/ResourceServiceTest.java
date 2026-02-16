package com.ramil.booking.resource_booking.domain.resource.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ramil.booking.resource_booking.domain.resource.dto.CreateResourceCommand;
import com.ramil.booking.resource_booking.domain.resource.dto.ResourceView;
import com.ramil.booking.resource_booking.domain.resource.dto.UpdateResourceCommand;
import com.ramil.booking.resource_booking.domain.resource.entity.ResourceEntity;
import com.ramil.booking.resource_booking.domain.resource.exception.InvalidResourceNameException;
import com.ramil.booking.resource_booking.domain.resource.exception.InvalidResourceDescriptionException;
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceNotFoundException;
import com.ramil.booking.resource_booking.domain.resource.mapper.ResourceMapper;
import com.ramil.booking.resource_booking.domain.resource.repository.ResourceRepository;

class ResourceServiceTest {

    private final ResourceRepository resourceRepository = mock(ResourceRepository.class);
    private final ResourceMapper resourceMapper = mock(ResourceMapper.class);
    private final ResourceService resourceService = new ResourceService(resourceRepository, resourceMapper);

    @Test
    void create_creates_active_resource() {
        UUID id = UUID.randomUUID();
        CreateResourceCommand cmd = new CreateResourceCommand("Room A", "Conference room");
        ResourceEntity saved = new ResourceEntity(id, "Room A", "Conference room", true);
        ResourceView expectedView = new ResourceView(id, "Room A", "Conference room", true);

        when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(saved);
        when(resourceMapper.toView(saved)).thenReturn(expectedView);

        ResourceView result = resourceService.create(cmd);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Room A");
        assertThat(result.description()).isEqualTo("Conference room");
        assertThat(result.active()).isTrue();
        verify(resourceRepository).save(any(ResourceEntity.class));
    }

    @Test
    void create_throws_if_name_null() {
        assertThatThrownBy(() -> resourceService.create(new CreateResourceCommand(null, "desc")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name must not be null");

        verifyNoInteractions(resourceRepository);
    }

    @Test
    void create_throws_if_name_blank() {
        assertThatThrownBy(() -> resourceService.create(new CreateResourceCommand("  ", "desc")))
                .isInstanceOf(InvalidResourceNameException.class);

        verifyNoInteractions(resourceRepository);
    }

    @Test
    void create_throws_if_name_too_long() {
        String longName = "a".repeat(101);
        assertThatThrownBy(() -> resourceService.create(new CreateResourceCommand(longName, "desc")))
                .isInstanceOf(InvalidResourceNameException.class);

        verifyNoInteractions(resourceRepository);
    }

    @Test
    void create_accepts_null_description() {
        UUID id = UUID.randomUUID();
        CreateResourceCommand cmd = new CreateResourceCommand("Room A", null);
        ResourceEntity saved = new ResourceEntity(id, "Room A", null, true);
        ResourceView expectedView = new ResourceView(id, "Room A", null, true);

        when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(saved);
        when(resourceMapper.toView(saved)).thenReturn(expectedView);

        ResourceView result = resourceService.create(cmd);
        assertThat(result.description()).isNull();
    }

    @Test
    void update_updates_resource() {
        UUID id = UUID.randomUUID();
        UpdateResourceCommand cmd = new UpdateResourceCommand(id, "Updated Room", "Updated desc");
        ResourceEntity entity = new ResourceEntity(id, "Room A", "desc", true);
        ResourceView expectedView = new ResourceView(id, "Updated Room", "Updated desc", true);

        when(resourceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(resourceMapper.toView(entity)).thenReturn(expectedView);

        ResourceView result = resourceService.update(cmd);

        assertThat(result.name()).isEqualTo("Updated Room");
        assertThat(result.description()).isEqualTo("Updated desc");
        assertThat(entity.getName()).isEqualTo("Updated Room");
        assertThat(entity.getDescription()).isEqualTo("Updated desc");
    }

    @Test
    void update_throws_if_resource_not_found() {
        UUID id = UUID.randomUUID();
        UpdateResourceCommand cmd = new UpdateResourceCommand(id, "Updated", null);
        when(resourceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.update(cmd))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resourceRepository).findById(id);
    }

    @Test
    void delete_removes_resource() {
        UUID id = UUID.randomUUID();
        when(resourceRepository.existsById(id)).thenReturn(true);

        resourceService.delete(id);

        verify(resourceRepository).deleteById(id);
    }

    @Test
    void delete_throws_if_resource_not_found() {
        UUID id = UUID.randomUUID();
        when(resourceRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> resourceService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resourceRepository, never()).deleteById(any());
    }

    @Test
    void activate_sets_active_true() {
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity(id, "Room", null, false);
        ResourceView expectedView = new ResourceView(id, "Room", null, true);

        when(resourceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(resourceMapper.toView(entity)).thenReturn(expectedView);

        ResourceView result = resourceService.activate(id);

        assertThat(entity.isActive()).isTrue();
        assertThat(result.active()).isTrue();
    }

    @Test
    void deactivate_sets_active_false() {
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity(id, "Room", null, true);
        ResourceView expectedView = new ResourceView(id, "Room", null, false);

        when(resourceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(resourceMapper.toView(entity)).thenReturn(expectedView);

        ResourceView result = resourceService.deactivate(id);

        assertThat(entity.isActive()).isFalse();
        assertThat(result.active()).isFalse();
    }

    @Test
    void getById_returns_resource() {
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity(id, "Room", "desc", true);
        ResourceView expectedView = new ResourceView(id, "Room", "desc", true);

        when(resourceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(resourceMapper.toView(entity)).thenReturn(expectedView);

        ResourceView result = resourceService.getById(id);

        assertThat(result.id()).isEqualTo(id);
        verify(resourceRepository).findById(id);
    }

    @Test
    void getById_throws_if_not_found() {
        UUID id = UUID.randomUUID();
        when(resourceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_returns_all_when_active_null() {
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity(id, "Room", null, true);
        ResourceView view = new ResourceView(id, "Room", null, true);

        when(resourceRepository.findAll()).thenReturn(List.of(entity));
        when(resourceMapper.toView(entity)).thenReturn(view);

        List<ResourceView> result = resourceService.list(null);

        assertThat(result).hasSize(1);
        verify(resourceRepository).findAll();
    }

    @Test
    void list_returns_filtered_when_active_specified() {
        UUID id = UUID.randomUUID();
        ResourceEntity entity = new ResourceEntity(id, "Room", null, true);
        ResourceView view = new ResourceView(id, "Room", null, true);

        when(resourceRepository.findByActive(true)).thenReturn(List.of(entity));
        when(resourceMapper.toView(entity)).thenReturn(view);

        List<ResourceView> result = resourceService.list(true);

        assertThat(result).hasSize(1);
        verify(resourceRepository).findByActive(true);
    }

    @Test
    void update_throws_if_description_too_long() {
        UUID id = UUID.randomUUID();
        String longDesc = "a".repeat(1001);
        ResourceEntity entity = new ResourceEntity(id, "Room", null, true);
        UpdateResourceCommand cmd = new UpdateResourceCommand(id, "Room", longDesc);

        when(resourceRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> resourceService.update(cmd))
                .isInstanceOf(InvalidResourceDescriptionException.class);
    }
}

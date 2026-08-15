package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.UpdateRoleRequest;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateRoleServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @InjectMocks
    private UpdateRoleService updateRoleService;

    private UUID petId;
    private UUID ownerId;
    private UUID targetUserId;
    private Pet pet;
    private PetUserAccess access;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        pet = Pet.builder()
                .id(petId)
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        access = PetUserAccess.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .userId(targetUserId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnUpdatedRole_whenRequestIsValid() {
        PetUserAccess updated = access.toBuilder().accessRole(Rol.VIEWER).build();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId))
                .thenReturn(Optional.of(access));
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(updated);

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .targetUserId(targetUserId)
                .newRole(Rol.VIEWER)
                .build();

        UpdateRoleResponse result = updateRoleService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getUserId()).isEqualTo(targetUserId);
        assertThat(result.getNewRole()).isEqualTo(Rol.VIEWER);
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        PetUserAccess updated = access.toBuilder().accessRole(Rol.VIEWER).build();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId))
                .thenReturn(Optional.of(access));
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(updated);

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).newRole(Rol.VIEWER).build();

        updateRoleService.execute(request);

        verify(petUserAccessRepositoryPort, times(1)).save(any(PetUserAccess.class));
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).newRole(Rol.VIEWER).build();

        assertThatThrownBy(() -> updateRoleService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowUnauthorizedPetAccessException_whenRequesterIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .petId(petId).ownerId(otherUser).targetUserId(targetUserId).newRole(Rol.VIEWER).build();

        assertThatThrownBy(() -> updateRoleService.execute(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowAccessNotFoundException_whenAccessDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId))
                .thenReturn(Optional.empty());

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).newRole(Rol.VIEWER).build();

        assertThatThrownBy(() -> updateRoleService.execute(request))
                .isInstanceOf(AccessNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldNotCallSave_whenPetNotFound() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).newRole(Rol.VIEWER).build();

        assertThatThrownBy(() -> updateRoleService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }
}
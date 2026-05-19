package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GrantAccessRequest;
import com.MyAnimaLog.Pets.application.dto.GrantAccessResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.AccessAlreadyGrantedException;
import com.MyAnimaLog.Pets.domain.exceptions.CannotGrantAccessToOwnerException;
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
class GrantAccessServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @InjectMocks
    private GrantAccessService grantAccessService;

    private UUID petId;
    private UUID ownerId;
    private UUID targetUserId;
    private Pet pet;
    private PetUserAccess savedAccess;

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

        savedAccess = PetUserAccess.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .userId(targetUserId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnResponse_whenRequestIsValid() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId)).thenReturn(Optional.empty());
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(savedAccess);

        GrantAccessRequest request = GrantAccessRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .targetUserId(targetUserId)
                .accessRole(Rol.EDITOR)
                .build();

        GrantAccessResponse result = grantAccessService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getUserId()).isEqualTo(targetUserId);
        assertThat(result.getAccessRole()).isEqualTo(Rol.EDITOR);
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId)).thenReturn(Optional.empty());
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(savedAccess);

        GrantAccessRequest request = GrantAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).accessRole(Rol.EDITOR).build();

        grantAccessService.execute(request);

        verify(petUserAccessRepositoryPort, times(1)).save(any(PetUserAccess.class));
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        GrantAccessRequest request = GrantAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> grantAccessService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowUnauthorizedPetAccessException_whenRequesterIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        GrantAccessRequest request = GrantAccessRequest.builder()
                .petId(petId).ownerId(otherUser).targetUserId(targetUserId).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> grantAccessService.execute(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowCannotGrantAccessToOwnerException_whenTargetIsOwner() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        GrantAccessRequest request = GrantAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(ownerId).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> grantAccessService.execute(request))
                .isInstanceOf(CannotGrantAccessToOwnerException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowAccessAlreadyGrantedException_whenAccessAlreadyExists() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId))
                .thenReturn(Optional.of(savedAccess));

        GrantAccessRequest request = GrantAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> grantAccessService.execute(request))
                .isInstanceOf(AccessAlreadyGrantedException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldNotCallSave_whenPetNotFound() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        GrantAccessRequest request = GrantAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> grantAccessService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }
}
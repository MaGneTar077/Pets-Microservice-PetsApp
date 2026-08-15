package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.RevokeAccessRequest;
import com.MyAnimaLog.Pets.application.dto.RevokeAccessResponse;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokeAccessServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @InjectMocks
    private RevokeAccessService revokeAccessService;

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
    void execute_shouldRevokeAccessSuccessfully_whenRequestIsValid() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId))
                .thenReturn(Optional.of(access));
        doNothing().when(petUserAccessRepositoryPort).deleteByPetIdAndUserId(petId, targetUserId);

        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .targetUserId(targetUserId)
                .build();

        RevokeAccessResponse result = revokeAccessService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Access revoked successfully");
    }

    @Test
    void execute_shouldCallDeleteByPetIdAndUserId_once() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId))
                .thenReturn(Optional.of(access));
        doNothing().when(petUserAccessRepositoryPort).deleteByPetIdAndUserId(petId, targetUserId);

        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).build();

        revokeAccessService.execute(request);

        verify(petUserAccessRepositoryPort, times(1)).deleteByPetIdAndUserId(petId, targetUserId);
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).build();

        assertThatThrownBy(() -> revokeAccessService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).deleteByPetIdAndUserId(any(), any());
    }

    @Test
    void execute_shouldThrowUnauthorizedPetAccessException_whenRequesterIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .petId(petId).ownerId(otherUser).targetUserId(targetUserId).build();

        assertThatThrownBy(() -> revokeAccessService.execute(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petUserAccessRepositoryPort, never()).deleteByPetIdAndUserId(any(), any());
    }

    @Test
    void execute_shouldThrowAccessNotFoundException_whenAccessDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, targetUserId))
                .thenReturn(Optional.empty());

        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).build();

        assertThatThrownBy(() -> revokeAccessService.execute(request))
                .isInstanceOf(AccessNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).deleteByPetIdAndUserId(any(), any());
    }

    @Test
    void execute_shouldNotCallDelete_whenPetNotFound() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .petId(petId).ownerId(ownerId).targetUserId(targetUserId).build();

        assertThatThrownBy(() -> revokeAccessService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).deleteByPetIdAndUserId(any(), any());
    }
}
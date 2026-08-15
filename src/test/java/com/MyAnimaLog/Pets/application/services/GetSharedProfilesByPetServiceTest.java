package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesByPetRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSharedProfilesByPetServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @InjectMocks
    private GetSharedProfilesByPetService getSharedProfilesByPetService;

    private UUID petId;
    private UUID ownerId;
    private Pet pet;
    private PetUserAccess access;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

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
                .userId(UUID.randomUUID())
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnList_whenRequestIsValid() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findAllByPetId(petId)).thenReturn(List.of(access));

        GetSharedProfilesByPetRequest request = GetSharedProfilesByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        List<SharedProfileResponse> result = getSharedProfilesByPetService.execute(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPetId()).isEqualTo(petId);
        assertThat(result.get(0).getAccessRole()).isEqualTo(Rol.EDITOR);
    }

    @Test
    void execute_shouldReturnEmptyList_whenNoSharedProfiles() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findAllByPetId(petId)).thenReturn(Collections.emptyList());

        GetSharedProfilesByPetRequest request = GetSharedProfilesByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        List<SharedProfileResponse> result = getSharedProfilesByPetService.execute(request);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_shouldCallRepositoryFindAllByPetId_once() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findAllByPetId(petId)).thenReturn(List.of(access));

        GetSharedProfilesByPetRequest request = GetSharedProfilesByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        getSharedProfilesByPetService.execute(request);

        verify(petUserAccessRepositoryPort, times(1)).findAllByPetId(petId);
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        GetSharedProfilesByPetRequest request = GetSharedProfilesByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        assertThatThrownBy(() -> getSharedProfilesByPetService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).findAllByPetId(any());
    }

    @Test
    void execute_shouldThrowUnauthorizedPetAccessException_whenRequesterIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        GetSharedProfilesByPetRequest request = GetSharedProfilesByPetRequest.builder()
                .petId(petId).ownerId(otherUser).build();

        assertThatThrownBy(() -> getSharedProfilesByPetService.execute(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petUserAccessRepositoryPort, never()).findAllByPetId(any());
    }

    @Test
    void execute_shouldReturnMultipleProfiles_whenMultipleSharedProfilesExist() {
        PetUserAccess access2 = access.toBuilder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .accessRole(Rol.VIEWER)
                .build();

        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petUserAccessRepositoryPort.findAllByPetId(petId)).thenReturn(List.of(access, access2));

        GetSharedProfilesByPetRequest request = GetSharedProfilesByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        List<SharedProfileResponse> result = getSharedProfilesByPetService.execute(request);

        assertThat(result).hasSize(2);
    }
}
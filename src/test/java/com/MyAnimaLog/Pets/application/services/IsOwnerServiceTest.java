package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
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
class IsOwnerServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @InjectMocks
    private IsOwnerService isOwnerService;

    private UUID petId;
    private UUID ownerId;
    private Pet pet;

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
    }

    @Test
    void execute_shouldReturnIsOwnerTrue_whenUserIsOwner() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        IsOwnerRequest request = IsOwnerRequest.builder()
                .petId(petId)
                .userId(ownerId)
                .build();

        IsOwnerResponse result = isOwnerService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isOwner()).isTrue();
    }

    @Test
    void execute_shouldReturnIsOwnerFalse_whenUserIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        IsOwnerRequest request = IsOwnerRequest.builder()
                .petId(petId)
                .userId(otherUser)
                .build();

        IsOwnerResponse result = isOwnerService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isOwner()).isFalse();
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        IsOwnerRequest request = IsOwnerRequest.builder()
                .petId(petId)
                .userId(ownerId)
                .build();

        assertThatThrownBy(() -> isOwnerService.execute(request))
                .isInstanceOf(PetNotFoundException.class);
    }

    @Test
    void execute_shouldCallRepositoryFindById_once() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        IsOwnerRequest request = IsOwnerRequest.builder()
                .petId(petId)
                .userId(ownerId)
                .build();

        isOwnerService.execute(request);

        verify(petRepositoryPort, times(1)).findById(petId);
    }
}
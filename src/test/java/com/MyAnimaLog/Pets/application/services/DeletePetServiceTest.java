package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotBelongsToOwnerException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePetServiceTest {

    @Mock
    private PetRepositoryPort petRepository;

    @InjectMocks
    private DeletePetService deletePetService;

    private Pet pet;
    private UUID petId;
    private UUID ownerId;

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
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void deletePet_shouldDeleteSuccessfully_whenPetExistsAndBelongsToOwner() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(pet));

        assertThatNoException().isThrownBy(() ->
                deletePetService.deletePet(petId, ownerId)
        );

        verify(petRepository, times(1)).deleteById(petId);
    }

    @Test
    void deletePet_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.empty());
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                deletePetService.deletePet(petId, ownerId)
        ).isInstanceOf(PetNotFoundException.class);

        verify(petRepository, never()).deleteById(any());
    }

    @Test
    void deletePet_shouldThrowPetNotBelongsToOwnerException_whenOwnerMismatch() {
        UUID otherOwner = UUID.randomUUID();

        when(petRepository.findByIdAndOwnerId(petId, otherOwner)).thenReturn(Optional.empty());
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        assertThatThrownBy(() ->
                deletePetService.deletePet(petId, otherOwner)
        ).isInstanceOf(PetNotBelongsToOwnerException.class);

        verify(petRepository, never()).deleteById(any());
    }

    @Test
    void deletePet_shouldCallDeleteById_once() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(pet));

        deletePetService.deletePet(petId, ownerId);

        verify(petRepository, times(1)).deleteById(petId);
    }
}
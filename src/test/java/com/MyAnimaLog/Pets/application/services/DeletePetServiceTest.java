package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePetServiceTest {

    @Mock
    private PetRepositoryPort petRepository;

    @Mock
    private PetAccessPolicy petAccessPolicy;

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
    void deletePet_shouldDeleteSuccessfully_whenCallerIsTheOwner() {
        when(petAccessPolicy.requireOwner(petId, ownerId)).thenReturn(pet);

        assertThatNoException().isThrownBy(() -> deletePetService.deletePet(petId, ownerId));

        verify(petRepository, times(1)).deleteById(petId);
    }

    @Test
    void deletePet_shouldWork_whenTheRequestDoesNotSendOwnerId() {
        when(petAccessPolicy.requireOwner(petId, null)).thenReturn(pet);

        deletePetService.deletePet(petId, null);

        verify(petRepository, times(1)).deleteById(petId);
    }

    @Test
    void deletePet_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petAccessPolicy.requireOwner(petId, ownerId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() -> deletePetService.deletePet(petId, ownerId))
                .isInstanceOf(PetNotFoundException.class);

        verify(petRepository, never()).deleteById(any());
    }

    /** Un EDITOR tampoco puede borrar: la política solo deja al dueño. */
    @Test
    void deletePet_shouldThrow403_whenCallerIsNotTheOwner() {
        when(petAccessPolicy.requireOwner(petId, ownerId)).thenThrow(new PetNotBelongsToOwnerException());

        assertThatThrownBy(() -> deletePetService.deletePet(petId, ownerId))
                .isInstanceOf(PetNotBelongsToOwnerException.class);

        verify(petRepository, never()).deleteById(any());
    }

    @Test
    void deletePet_shouldThrow403_whenTheRequestSendsAnUnrelatedOwnerId() {
        UUID unrelatedOwner = UUID.randomUUID();
        when(petAccessPolicy.requireOwner(petId, unrelatedOwner)).thenThrow(new IdentityMismatchException());

        assertThatThrownBy(() -> deletePetService.deletePet(petId, unrelatedOwner))
                .isInstanceOf(IdentityMismatchException.class);

        verify(petRepository, never()).deleteById(any());
    }

    /** La app vieja mandaba el ownerId real; sigue aceptándose, pero no da permisos de dueño. */
    @Test
    void deletePet_shouldThrow403_whenEditorSendsTheRealOwnerId() {
        when(petAccessPolicy.requireOwner(petId, ownerId)).thenThrow(new PetNotBelongsToOwnerException());

        assertThatThrownBy(() -> deletePetService.deletePet(petId, ownerId))
                .isInstanceOf(PetNotBelongsToOwnerException.class);

        verify(petRepository, never()).deleteById(any());
    }
}

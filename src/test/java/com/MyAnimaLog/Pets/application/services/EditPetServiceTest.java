package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.EditPetRequest;
import com.MyAnimaLog.Pets.application.dto.EditPetResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
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
class EditPetServiceTest {

    @Mock
    private PetRepositoryPort petRepository;

    @Mock
    private PublishPetEventUseCase publishPetEventUseCase;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @Mock
    private PetAccessPolicy petAccessPolicy;

    @InjectMocks
    private EditPetService editPetService;

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
    void editPet_shouldUpdateFieldsSuccessfully() {
        EditPetRequest request = new EditPetRequest();
        request.setName("Firulais Editado");
        request.setWeight(15.0);

        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(petRepository.save(any(Pet.class))).thenReturn(pet);

        EditPetResponse response = editPetService.editPet(petId, ownerId, request);

        assertThat(response.getName()).isEqualTo("Firulais Editado");
        assertThat(response.getWeight()).isEqualTo(15.0);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void editPet_shouldWork_whenTheRequestDoesNotSendOwnerId() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(petRepository.save(any(Pet.class))).thenReturn(pet);

        editPetService.editPet(petId, null, new EditPetRequest());

        verify(currentUserIdentity).resolveUserId(null);
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    void editPet_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petAccessPolicy.requireWrite(petId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() -> editPetService.editPet(petId, ownerId, new EditPetRequest()))
                .isInstanceOf(PetNotFoundException.class);
    }

    /** Un VIEWER o un extraño no pueden editar: lo decide la política, no el ownerId del request. */
    @Test
    void editPet_shouldThrow403_whenCallerHasNoWriteAccess() {
        when(petAccessPolicy.requireWrite(petId)).thenThrow(new UnauthorizedPetAccessException());

        assertThatThrownBy(() -> editPetService.editPet(petId, ownerId, new EditPetRequest()))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void editPet_shouldThrow403_whenTheRequestSendsAnotherOwnerId() {
        UUID otherOwner = UUID.randomUUID();
        when(currentUserIdentity.resolveUserId(otherOwner)).thenThrow(new IdentityMismatchException());

        assertThatThrownBy(() -> editPetService.editPet(petId, otherOwner, new EditPetRequest()))
                .isInstanceOf(IdentityMismatchException.class);

        verifyNoInteractions(petAccessPolicy);
        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void editPet_shouldThrowInvalidPetDataException_whenWeightIsNegative() {
        EditPetRequest request = new EditPetRequest();
        request.setWeight(-10.0);

        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);

        assertThatThrownBy(() -> editPetService.editPet(petId, ownerId, request))
                .isInstanceOf(InvalidPetDataException.class);
    }

    @Test
    void editPet_shouldCallSave_once() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(petRepository.save(any(Pet.class))).thenReturn(pet);

        editPetService.editPet(petId, ownerId, new EditPetRequest());

        verify(petRepository, times(1)).save(any(Pet.class));
    }
}

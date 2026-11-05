package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetPhotoUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetPhotoStoragePort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DeletePetPhotoService implements DeletePetPhotoUseCase {

    private final PetRepositoryPort petRepository;
    private final PetPhotoStoragePort photoStorage;
    private final CurrentUserIdentity currentUserIdentity;
    private final PetAccessPolicy petAccessPolicy;

    /** {@code request.ownerId} es obsoleto (PETS-02): autoriza el token, dueño o {@code EDITOR}. */
    @Override
    public DeletePetPhotoResponse execute(DeletePetPhotoRequest request) {

        currentUserIdentity.resolveUserId(request.getOwnerId());
        Pet pet = petAccessPolicy.requireWrite(request.getPetId());

        if (pet.getPhotoUrl() == null || pet.getPhotoUrl().isBlank()) {
            throw new InvalidPetDataException("Pet does not have a photo to delete");
        }

        photoStorage.delete(pet.getId(), pet.getPhotoUrl());

        Pet updated = pet.toBuilder()
                .photoUrl(null)
                .updatedAt(LocalDateTime.now())
                .build();

        petRepository.save(updated);

        return DeletePetPhotoResponse.builder()
                .petId(pet.getId())
                .message("Photo deleted successfully")
                .build();
    }
}

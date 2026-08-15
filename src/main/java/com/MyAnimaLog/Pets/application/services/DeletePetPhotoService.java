package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetPhotoUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetPhotoStoragePort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
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

    @Override
    public DeletePetPhotoResponse execute(DeletePetPhotoRequest request) {

        Pet pet = petRepository
                .findByIdAndOwnerId(request.getPetId(), request.getOwnerId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Pet not found or does not belong to the owner"));

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

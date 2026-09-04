package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.EditPetRequest;
import com.MyAnimaLog.Pets.application.dto.EditPetResponse;
import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.ports.in.EditPetUseCase;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotBelongsToOwnerException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EditPetService  implements EditPetUseCase {

    private final PetRepositoryPort petRepository;
    private final PublishPetEventUseCase publishPetEventUseCase;

    public EditPetService(PetRepositoryPort petRepository, PublishPetEventUseCase publishPetEventUseCase) {
        this.petRepository = petRepository;
        this.publishPetEventUseCase = publishPetEventUseCase;
    }

    @Override
    public EditPetResponse editPet(UUID petId, UUID ownerId, EditPetRequest request) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(petId.toString()));

        if (!pet.getOwnerId().equals(ownerId)) {
            throw new PetNotBelongsToOwnerException();
        }

        validateRequest(request);

        if (request.getName() != null) pet.setName(request.getName());
        if (request.getSpecies() != null) pet.setSpecies(request.getSpecies());
        if (request.getBreed() != null) pet.setBreed(request.getBreed());

        if (request.getSex() != null) {
            try {
                pet.setSex(Sex.valueOf(request.getSex().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new InvalidPetDataException("Invalid sex value");
            }
        }

        if (request.getBirthDate() != null) pet.setBirthDate(request.getBirthDate());
        if (request.getHeight() != null) pet.setHeight(request.getHeight());
        if (request.getWeight() != null) pet.setWeight(request.getWeight());
        if (request.getPhotoUrl() != null) pet.setPhotoUrl(request.getPhotoUrl());

        pet.setUpdatedAt(LocalDateTime.now());

        Pet updatedPet = petRepository.save(pet);

        publishPetEventUseCase.publish(PetEvent.builder()
                .petId(updatedPet.getId())
                .ownerId(updatedPet.getOwnerId())
                .petName(updatedPet.getName())
                .eventType("PET_EDITED")
                .occurredAt(Instant.now())
                .build());

        return mapToResponse(updatedPet);
    }

    private void validateRequest(EditPetRequest request) {

        if (request.getName() != null && request.getName().isBlank()) {
            throw new InvalidPetDataException("Name cannot be empty");
        }

        if (request.getWeight() != null && request.getWeight() <= 0) {
            throw new InvalidPetDataException("Weight must be greater than 0");
        }

        if (request.getHeight() != null && request.getHeight() <= 0) {
            throw new InvalidPetDataException("Height must be greater than 0");
        }
    }

    private EditPetResponse mapToResponse(Pet pet) {
        return EditPetResponse.builder()
                .id(pet.getId())
                .ownerId(pet.getOwnerId())
                .name(pet.getName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .sex(pet.getSex())
                .birthDate(pet.getBirthDate())
                .height(pet.getHeight())
                .weight(pet.getWeight())
                .photoUrl(pet.getPhotoUrl())
                .createdAt(pet.getCreatedAt())
                .updatedAt(pet.getUpdatedAt())
                .build();
    }
}

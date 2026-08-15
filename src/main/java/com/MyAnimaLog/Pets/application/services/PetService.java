package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.AddPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.application.ports.in.AddPetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PetService implements AddPetUseCase {

    private final PetRepositoryPort petRepositoryPort;

    @Override
    public Pet addPet(Pet pet) {
        pet.setCreatedAt(LocalDateTime.now());
        return petRepositoryPort.save(pet);
    }

    public PetResponse add(AddPetRequest request) {

        Pet pet = Pet.builder()
                .ownerId(request.getOwnerId())
                .name(request.getName())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .sex(request.getSex())
                .birthDate(request.getBirthDate())
                .height(request.getHeight())
                .weight(request.getWeight())
                .build();

        Pet saved = addPet(pet);

        return toResponse(saved);
    }

    private PetResponse toResponse(Pet pet) {
        return PetResponse.builder()
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
                .build();
    }
}
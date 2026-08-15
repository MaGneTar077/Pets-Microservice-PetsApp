package com.MyAnimaLog.Pets.infrastructure.mapper;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.entity.PetEntity;
import org.springframework.stereotype.Component;

@Component
public class PetMapper {
    public PetEntity toEntity(Pet pet) {
        return PetEntity.builder()
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

    public Pet toDomain(PetEntity entity) {
        return Pet.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .name(entity.getName())
                .species(entity.getSpecies())
                .breed(entity.getBreed())
                .sex(entity.getSex())
                .birthDate(entity.getBirthDate())
                .height(entity.getHeight())
                .weight(entity.getWeight())
                .photoUrl(entity.getPhotoUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PetResponse toResponse(Pet pet) {
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

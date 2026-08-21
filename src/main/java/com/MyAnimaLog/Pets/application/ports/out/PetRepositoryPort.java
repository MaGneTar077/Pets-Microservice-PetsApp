package com.MyAnimaLog.Pets.application.ports.out;

import com.MyAnimaLog.Pets.domain.model.Pet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetRepositoryPort {
    Pet save(Pet pet);
    Optional<Pet> findById(UUID id);
    Optional<Pet> findByIdAndOwnerId(UUID petId, UUID ownerId);
    List<Pet> findAllByOwnerId(UUID ownerId);
    void deleteById(UUID id);
}

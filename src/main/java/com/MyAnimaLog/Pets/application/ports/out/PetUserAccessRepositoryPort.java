package com.MyAnimaLog.Pets.application.ports.out;

import com.MyAnimaLog.Pets.domain.model.PetUserAccess;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetUserAccessRepositoryPort {
    PetUserAccess save(PetUserAccess petUserAccess);
    Optional<PetUserAccess> findByPetIdAndUserId(UUID petId, UUID userId);
    void deleteByPetIdAndUserId(UUID petId, UUID userId);
    List<PetUserAccess> findAllByUserId(UUID userId);
    List<PetUserAccess> findAllByPetId(UUID petId);
}

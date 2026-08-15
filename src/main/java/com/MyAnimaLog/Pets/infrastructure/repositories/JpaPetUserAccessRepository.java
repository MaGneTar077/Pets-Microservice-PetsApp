package com.MyAnimaLog.Pets.infrastructure.repositories;

import com.MyAnimaLog.Pets.infrastructure.entity.PetUserAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPetUserAccessRepository extends JpaRepository<PetUserAccessEntity, UUID> {
    Optional<PetUserAccessEntity> findByPetIdAndUserId(UUID petId, UUID userId);
    void deleteByPetIdAndUserId(UUID petId, UUID userId);
    List<PetUserAccessEntity> findAllByUserId(UUID userId);
    List<PetUserAccessEntity> findAllByPetId(UUID petId);
}
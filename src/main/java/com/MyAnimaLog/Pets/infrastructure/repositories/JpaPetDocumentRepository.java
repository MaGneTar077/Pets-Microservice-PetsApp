package com.MyAnimaLog.Pets.infrastructure.repositories;

import com.MyAnimaLog.Pets.infrastructure.entity.PetDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPetDocumentRepository extends JpaRepository<PetDocumentEntity, UUID> {
    List<PetDocumentEntity> findAllByPetId(UUID petId);
    Optional<PetDocumentEntity> findByIdAndPetId(UUID id, UUID petId);
}

package com.MyAnimaLog.Pets.application.ports.out;

import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.model.PetDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetDocumentRepositoryPort {
    PetDocument save(PetDocument document);
    Optional<PetDocument> findById(UUID id);
    Optional<PetDocument> findByIdAndPetId(UUID documentId, UUID petId);
    List<PetDocument> findAllByPetId(UUID petId);
    void deleteById(UUID id);
    List<PetDocument> findAllByPetIdAndDocumentType(UUID petId, DocumentType documentType);
}

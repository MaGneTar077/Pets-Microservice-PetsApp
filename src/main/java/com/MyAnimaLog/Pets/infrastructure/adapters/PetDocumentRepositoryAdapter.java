package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetDocumentMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PetDocumentRepositoryAdapter implements PetDocumentRepositoryPort {

    private final JpaPetDocumentRepository jpaRepository;
    private final PetDocumentMapper mapper;

    @Override
    public PetDocument save(PetDocument document) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(document))
        );
    }

    @Override
    public Optional<PetDocument> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PetDocument> findByIdAndPetId(UUID documentId, UUID petId) {
        return jpaRepository.findByIdAndPetId(documentId, petId).map(mapper::toDomain);
    }

    @Override
    public List<PetDocument> findAllByPetId(UUID petId) {
        return jpaRepository.findAllByPetId(petId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<PetDocument> findAllByPetIdAndDocumentType(UUID petId, DocumentType documentType) {
        return jpaRepository.findAllByPetIdAndDocumentType(petId, documentType)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

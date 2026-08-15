package com.MyAnimaLog.Pets.application.services;


import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataRequest;
import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataResponse;
import com.MyAnimaLog.Pets.application.ports.in.UpdatePetDocumentMetadataUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidDocumentException;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdatePetDocumentMetadataService implements UpdatePetDocumentMetadataUseCase {

    private final PetDocumentRepositoryPort documentRepository;

    @Override
    public UpdatePetDocumentMetadataResponse execute(UpdatePetDocumentMetadataRequest request) {
        PetDocument document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + request.getDocumentId()));

        if (!document.getPetId().equals(request.getPetId())) {
            throw new DocumentNotBelongsToPetException(
                    "Document does not belong to pet: " + request.getPetId());
        }

        if ((request.getTitle() == null || request.getTitle().isBlank()) &&
                (request.getDescription() == null || request.getDescription().isBlank())) {
            throw new InvalidDocumentException(
                    "At least one field (title or description) must be provided");
        }

        PetDocument updated = document.toBuilder()
                .title(request.getTitle() != null && !request.getTitle().isBlank()
                        ? request.getTitle() : document.getTitle())
                .description(request.getDescription() != null && !request.getDescription().isBlank()
                        ? request.getDescription() : document.getDescription())
                .build();

        PetDocument saved = documentRepository.save(updated);

        return toResponse(saved);
    }

    private UpdatePetDocumentMetadataResponse toResponse(PetDocument document) {
        return UpdatePetDocumentMetadataResponse.builder()
                .id(document.getId())
                .petId(document.getPetId())
                .title(document.getTitle())
                .description(document.getDescription())
                .documentType(document.getDocumentType())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}

package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetDocumentByIdUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPetDocumentByIdService implements GetPetDocumentByIdUseCase {

    private final PetDocumentRepositoryPort  documentRepository;

    @Override
    public GetPetDocumentResponse execute(UUID petId, UUID documentId) {
        PetDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + documentId));

        if (!document.getPetId().equals(petId)) {
            throw new DocumentNotBelongsToPetException(
                    "Document does not belong to pet: " + petId);
        }

        return toResponse(document);
    }

    private GetPetDocumentResponse toResponse(PetDocument document) {
        return GetPetDocumentResponse.builder()
                .id(document.getId())
                .petId(document.getPetId())
                .uploadedBy(document.getUploadedBy())
                .title(document.getTitle())
                .description(document.getDescription())
                .fileUrl(document.getFileUrl())
                .mimeType(document.getMimeType())
                .fileSizeBytes(document.getFileSizeBytes())
                .documentType(document.getDocumentType())
                .uploadedAt(document.getUploadedAt())
                .active(document.getActive())
                .build();
    }
}

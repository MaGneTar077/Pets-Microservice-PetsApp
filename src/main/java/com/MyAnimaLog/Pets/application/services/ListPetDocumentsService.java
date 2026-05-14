package com.MyAnimaLog.Pets.application.services;


import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.application.ports.in.ListPetDocumentsUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPetDocumentsService implements ListPetDocumentsUseCase {

    private final PetRepositoryPort petRepository;
    private final PetDocumentRepositoryPort petDocumentRepository;

    @Override
    public List<PetDocumentSummaryResponse> execute(UUID petId) {
        petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(
                        "Pet not found with id: " + petId));

        return petDocumentRepository.findAllByPetId(petId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PetDocumentSummaryResponse toResponse(PetDocument document) {
        return PetDocumentSummaryResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .fileUrl(document.getFileUrl())
                .mimeType(document.getMimeType())
                .fileSizeBytes(document.getFileSizeBytes())
                .documentType(document.getDocumentType())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}

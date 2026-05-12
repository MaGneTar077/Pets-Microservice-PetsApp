package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetDocumentUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentStoragePort;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeletePetDocumentService implements DeletePetDocumentUseCase {

    private final PetDocumentRepositoryPort documentRepository;
    private final PetDocumentStoragePort documentStorage;

    @Override
    public DeletePetDocumentResponse execute(DeletePetDocumentRequest request) {

        PetDocument document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + request.getDocumentId()));

        if (!document.getPetId().equals(request.getPetId())) {
            throw new DocumentNotBelongsToPetException(
                    "Document does not belong to pet: " + request.getPetId());
        }

        documentStorage.delete(document.getPetId(), document.getFileUrl());

        documentRepository.deleteById(document.getId());

        return DeletePetDocumentResponse.builder()
                .documentId(document.getId())
                .message("Document deleted successfully")
                .build();
    }
}

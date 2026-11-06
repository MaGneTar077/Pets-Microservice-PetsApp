package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetDocumentUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentStoragePort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
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
    private final CurrentUserIdentity currentUserIdentity;
    private final PetAccessPolicy petAccessPolicy;

    /**
     * {@code request.requestedBy} es obsoleto (PETS-02): se acepta si coincide con el token.
     * Autoriza dueño, o {@code EDITOR} solo si él mismo subió el documento.
     */
    @Override
    public DeletePetDocumentResponse execute(DeletePetDocumentRequest request) {

        currentUserIdentity.resolveUserId(request.getRequestedBy());

        PetDocument document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + request.getDocumentId()));

        if (!document.getPetId().equals(request.getPetId())) {
            throw new DocumentNotBelongsToPetException(
                    "Document does not belong to pet: " + request.getPetId());
        }

        petAccessPolicy.requireOwnerOrUploader(request.getPetId(), document.getUploadedBy());

        documentStorage.delete(document.getPetId(), document.getFileUrl());

        documentRepository.deleteById(document.getId());

        return DeletePetDocumentResponse.builder()
                .documentId(document.getId())
                .message("Document deleted successfully")
                .build();
    }
}

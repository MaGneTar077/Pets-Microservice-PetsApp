package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlRequest;
import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetDownloadUrlUseCase;
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
public class GetDownloadUrlService implements GetDownloadUrlUseCase {

    private final PetDocumentRepositoryPort petDocumentRepositoryPort;
    private final PetDocumentStoragePort petDocumentStoragePort;
    private final CurrentUserIdentity currentUserIdentity;
    private final PetAccessPolicy petAccessPolicy;

    /**
     * {@code request.requestedBy} es obsoleto (PETS-02) y nunca se verificaba: ahora solo se
     * acepta si coincide con el token (403 en otro caso). La URL firmada solo se genera si el
     * usuario del token tiene acceso de lectura a la mascota.
     */
    @Override
    public GetDownloadUrlResponse getDownloadUrl(GetDownloadUrlRequest request) {
        currentUserIdentity.resolveUserId(request.getRequestedBy());

        PetDocument document = petDocumentRepositoryPort
                .findById(request.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(request.getDocumentId()));

        if (!document.getPetId().equals(request.getPetId())) {
            throw new DocumentNotBelongsToPetException(request.getDocumentId(), request.getPetId());
        }

        petAccessPolicy.requireRead(request.getPetId());

        String downloadUrl = petDocumentStoragePort.getDownloadUrl(document.getFileUrl());

        return GetDownloadUrlResponse.builder()
                .documentId(document.getId())
                .petId(document.getPetId())
                .title(document.getTitle())
                .downloadUrl(downloadUrl)
                .mimeType(document.getMimeType())
                .fileSizeBytes(document.getFileSizeBytes())
                .build();
    }
}

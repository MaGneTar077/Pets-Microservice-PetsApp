package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GetPetDocumentResponse;

import java.util.UUID;

public interface GetPetDocumentByIdUseCase {
    GetPetDocumentResponse execute(UUID petId, UUID documentId);
}

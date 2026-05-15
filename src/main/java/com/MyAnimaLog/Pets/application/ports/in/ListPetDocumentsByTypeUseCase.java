package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;

import java.util.List;
import java.util.UUID;

public interface ListPetDocumentsByTypeUseCase {
    List<PetDocumentSummaryResponse> execute(UUID petId, DocumentType documentType);
}

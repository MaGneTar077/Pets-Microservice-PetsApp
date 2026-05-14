package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ListPetDocumentsUseCase {
    List<PetDocumentSummaryResponse> execute(UUID petId);
}

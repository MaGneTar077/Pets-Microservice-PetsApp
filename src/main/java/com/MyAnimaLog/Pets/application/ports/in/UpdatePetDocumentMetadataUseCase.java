package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataRequest;
import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataResponse;

public interface UpdatePetDocumentMetadataUseCase {
    UpdatePetDocumentMetadataResponse execute(UpdatePetDocumentMetadataRequest request);
}

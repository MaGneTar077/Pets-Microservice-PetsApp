package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentResponse;

public interface UploadPetDocumentUseCase {
    UploadPetDocumentResponse execute(UploadPetDocumentRequest request);
}

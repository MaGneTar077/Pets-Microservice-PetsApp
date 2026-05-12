package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentResponse;

public interface DeletePetDocumentUseCase {
    DeletePetDocumentResponse execute(DeletePetDocumentRequest request);
}

package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoResponse;

public interface UploadPetPhotoUseCase {
    UploadPetPhotoResponse execute(UploadPetPhotoRequest request);
}

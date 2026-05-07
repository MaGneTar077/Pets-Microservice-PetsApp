package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoResponse;

public interface DeletePetPhotoUseCase {
    DeletePetPhotoResponse execute(DeletePetPhotoRequest request);
}

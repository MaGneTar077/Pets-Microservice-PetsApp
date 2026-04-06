package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.EditPetRequest;
import com.MyAnimaLog.Pets.application.dto.EditPetResponse;

import java.util.UUID;

public interface EditPetUseCase {
    EditPetResponse editPet(UUID petId, UUID ownerId, EditPetRequest request);
}

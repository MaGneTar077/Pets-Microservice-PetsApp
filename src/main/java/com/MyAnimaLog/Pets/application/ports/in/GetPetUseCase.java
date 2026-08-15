package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.PetResponse;

import java.util.UUID;

public interface GetPetUseCase {
    PetResponse getPetById(UUID petId);
}

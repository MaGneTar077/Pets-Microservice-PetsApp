package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.PetResponse;

import java.util.List;
import java.util.UUID;

public interface GetPetsByUserIdUseCase {
    List<PetResponse> getPetsByUserId(UUID userId);
}

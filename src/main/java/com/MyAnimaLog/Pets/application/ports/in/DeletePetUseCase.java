package com.MyAnimaLog.Pets.application.ports.in;

import java.util.UUID;

public interface DeletePetUseCase {
    void deletePet(UUID petId, UUID ownerId);
}

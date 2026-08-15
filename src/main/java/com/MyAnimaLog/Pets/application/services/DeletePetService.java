package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.ports.in.DeletePetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotBelongsToOwnerException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeletePetService implements DeletePetUseCase {

    private final PetRepositoryPort petRepositoryPort;

    public DeletePetService(PetRepositoryPort petRepositoryPort) {
        this.petRepositoryPort = petRepositoryPort;
    }

    @Override
    public void deletePet(UUID petId, UUID ownerId) {
        Pet pet = petRepositoryPort.findByIdAndOwnerId(petId, ownerId)
                .orElseThrow(() -> {
                    petRepositoryPort.findById(petId)
                            .orElseThrow(PetNotFoundException::new);
                    return new PetNotBelongsToOwnerException();
                });

        petRepositoryPort.deleteById(pet.getId());
    }
}

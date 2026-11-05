package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.ports.in.DeletePetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeletePetService implements DeletePetUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final CurrentUserIdentity currentUserIdentity;
    private final PetAccessPolicy petAccessPolicy;

    public DeletePetService(PetRepositoryPort petRepositoryPort,
                            CurrentUserIdentity currentUserIdentity,
                            PetAccessPolicy petAccessPolicy) {
        this.petRepositoryPort = petRepositoryPort;
        this.currentUserIdentity = currentUserIdentity;
        this.petAccessPolicy = petAccessPolicy;
    }

    /**
     * @param ownerId <b>obsoleto</b> (PETS-02): solo se comprueba que coincida con el token. Borrar
     *                es exclusivo del dueño, ni siquiera un {@code EDITOR} puede.
     */
    @Override
    public void deletePet(UUID petId, UUID ownerId) {
        currentUserIdentity.resolveUserId(ownerId);
        Pet pet = petAccessPolicy.requireOwner(petId);

        petRepositoryPort.deleteById(pet.getId());
    }
}

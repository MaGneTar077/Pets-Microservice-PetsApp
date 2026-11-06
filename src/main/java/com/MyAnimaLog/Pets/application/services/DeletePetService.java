package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.ports.in.DeletePetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeletePetService implements DeletePetUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetAccessPolicy petAccessPolicy;

    public DeletePetService(PetRepositoryPort petRepositoryPort,
                            PetAccessPolicy petAccessPolicy) {
        this.petRepositoryPort = petRepositoryPort;
        this.petAccessPolicy = petAccessPolicy;
    }

    /**
     * @param ownerId <b>obsoleto</b> (PETS-02): se acepta si coincide con el token o con el dueño
     *                real de la mascota; cualquier otro valor es 403. Borrar sigue siendo exclusivo
     *                del dueño autenticado — un {@code EDITOR} que mande el ownerId correcto igual
     *                recibe 403 {@code NOT_PET_OWNER}.
     */
    @Override
    public void deletePet(UUID petId, UUID ownerId) {
        Pet pet = petAccessPolicy.requireOwner(petId, ownerId);

        petRepositoryPort.deleteById(pet.getId());
    }
}

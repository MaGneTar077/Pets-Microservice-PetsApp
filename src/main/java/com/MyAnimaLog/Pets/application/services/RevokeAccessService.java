package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.RevokeAccessRequest;
import com.MyAnimaLog.Pets.application.dto.RevokeAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.RevokeAccessUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.CannotGrantAccessToOwnerException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokeAccessService implements RevokeAccessUseCase {

    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final PetAccessPolicy petAccessPolicy;

    /**
     * {@code request.ownerId} es obsoleto (PETS-02): revocar sigue siendo exclusivo del dueño. El
     * dueño no tiene fila en {@code pet_user_access}: revocar su propio acceso no tiene sentido y
     * se responde con el mismo 400 que compartir o cambiar su rol, en vez de un 404 confuso.
     */
    @Override
    public RevokeAccessResponse execute(RevokeAccessRequest request) {
        Pet pet = petAccessPolicy.requireOwner(request.getPetId(), request.getOwnerId());

        if (request.getTargetUserId().equals(pet.getOwnerId())) {
            throw new CannotGrantAccessToOwnerException("Cannot revoke the access of the pet owner");
        }

        petUserAccessRepositoryPort.findByPetIdAndUserId(request.getPetId(), request.getTargetUserId())
                .orElseThrow(AccessNotFoundException::new);

        petUserAccessRepositoryPort.deleteByPetIdAndUserId(request.getPetId(), request.getTargetUserId());

        return RevokeAccessResponse.builder()
                .message("Access revoked successfully")
                .build();
    }
}

package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleRequest;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.in.UpdateRoleUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.CannotGrantAccessToOwnerException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateRoleService implements UpdateRoleUseCase {

    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final PublishPetEventUseCase publishPetEventUseCase;
    private final PetAccessPolicy petAccessPolicy;

    /**
     * {@code request.ownerId} es obsoleto (PETS-02): cambiar roles sigue siendo exclusivo del
     * dueño. El dueño no tiene fila en {@code pet_user_access} y no se le puede asignar un rol.
     */
    @Override
    public UpdateRoleResponse execute(UpdateRoleRequest request) {
        Pet pet = petAccessPolicy.requireOwner(request.getPetId(), request.getOwnerId());

        if (request.getTargetUserId().equals(pet.getOwnerId())) {
            throw new CannotGrantAccessToOwnerException("Cannot change the role of the pet owner");
        }

        PetUserAccess access = petUserAccessRepositoryPort
                .findByPetIdAndUserId(request.getPetId(), request.getTargetUserId())
                .orElseThrow(AccessNotFoundException::new);

        PetUserAccess updated = access.toBuilder()
                .accessRole(request.getNewRole())
                .build();

        PetUserAccess saved = petUserAccessRepositoryPort.save(updated);

        publishPetEventUseCase.publish(PetEvent.builder()
                .petId(pet.getId())
                .ownerId(pet.getOwnerId())
                .petName(pet.getName())
                .eventType("PET_ROLE_UPDATED")
                .occurredAt(Instant.now())
                .build());

        return UpdateRoleResponse.builder()
                .id(saved.getId())
                .petId(saved.getPetId())
                .userId(saved.getUserId())
                .newRole(saved.getAccessRole())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

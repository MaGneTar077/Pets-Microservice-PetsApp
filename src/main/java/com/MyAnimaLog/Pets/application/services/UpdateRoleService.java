package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleRequest;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.in.UpdateRoleUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateRoleService implements UpdateRoleUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final PublishPetEventUseCase publishPetEventUseCase;

    @Override
    public UpdateRoleResponse execute(UpdateRoleRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        if (!pet.getOwnerId().equals(request.getOwnerId())) {
            throw new UnauthorizedPetAccessException();
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

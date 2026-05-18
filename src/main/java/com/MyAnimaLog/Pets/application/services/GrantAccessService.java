package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GrantAccessRequest;
import com.MyAnimaLog.Pets.application.dto.GrantAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.GrantAccessUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.AccessAlreadyGrantedException;
import com.MyAnimaLog.Pets.domain.exceptions.CannotGrantAccessToOwnerException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GrantAccessService implements GrantAccessUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Override
    public GrantAccessResponse execute(GrantAccessRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        if (!pet.getOwnerId().equals(request.getOwnerId())) {
            throw new UnauthorizedPetAccessException();
        }

        if (request.getTargetUserId().equals(pet.getOwnerId())) {
            throw new CannotGrantAccessToOwnerException();
        }

        petUserAccessRepositoryPort.findByPetIdAndUserId(request.getPetId(), request.getTargetUserId())
                .ifPresent(existing -> { throw new AccessAlreadyGrantedException(); });

        PetUserAccess access = PetUserAccess.builder()
                .id(UUID.randomUUID())
                .petId(request.getPetId())
                .userId(request.getTargetUserId())
                .accessRole(request.getAccessRole())
                .createdAt(LocalDateTime.now())
                .build();

        PetUserAccess saved = petUserAccessRepositoryPort.save(access);

        return GrantAccessResponse.builder()
                .id(saved.getId())
                .petId(saved.getPetId())
                .userId(saved.getUserId())
                .accessRole(saved.getAccessRole())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}

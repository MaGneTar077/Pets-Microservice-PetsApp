package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesByPetRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesByPetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSharedProfilesByPetService implements GetSharedProfilesByPetUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Override
    public List<SharedProfileResponse> execute(GetSharedProfilesByPetRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        if (!pet.getOwnerId().equals(request.getOwnerId())) {
            throw new UnauthorizedPetAccessException();
        }

        return petUserAccessRepositoryPort
                .findAllByPetId(request.getPetId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SharedProfileResponse toResponse(PetUserAccess access) {
        return SharedProfileResponse.builder()
                .accessId(access.getId())
                .petId(access.getPetId())
                .userId(access.getUserId())
                .accessRole(access.getAccessRole())
                .createdAt(access.getCreatedAt())
                .build();
    }
}

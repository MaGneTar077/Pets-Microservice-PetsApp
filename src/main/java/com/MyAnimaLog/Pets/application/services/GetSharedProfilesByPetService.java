package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesByPetRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesByPetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSharedProfilesByPetService implements GetSharedProfilesByPetUseCase {

    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final PetAccessPolicy petAccessPolicy;

    /** {@code request.ownerId} es obsoleto (PETS-02): ver con quién se comparte es exclusivo del dueño. */
    @Override
    public List<SharedProfileResponse> execute(GetSharedProfilesByPetRequest request) {
        petAccessPolicy.requireOwner(request.getPetId(), request.getOwnerId());

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

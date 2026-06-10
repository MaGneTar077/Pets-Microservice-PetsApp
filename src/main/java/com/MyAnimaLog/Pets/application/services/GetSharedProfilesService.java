package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSharedProfilesService implements GetSharedProfilesUseCase {

    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Override
    public List<SharedProfileResponse> execute(GetSharedProfilesRequest request) {
        return petUserAccessRepositoryPort
                .findAllByUserId(request.getUserId())
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
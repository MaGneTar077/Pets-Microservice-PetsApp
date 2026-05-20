package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.HasPermissionRequest;
import com.MyAnimaLog.Pets.application.dto.HasPermissionResponse;
import com.MyAnimaLog.Pets.application.ports.in.HasPermissionUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HasPermissionService implements HasPermissionUseCase {

    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Override
    public HasPermissionResponse execute(HasPermissionRequest request) {
        Optional<PetUserAccess> access = petUserAccessRepositoryPort
                .findByPetIdAndUserId(request.getPetId(), request.getUserId());

        return access.map(a -> HasPermissionResponse.builder()
                        .hasPermission(true)
                        .role(a.getAccessRole())
                        .build())
                .orElse(HasPermissionResponse.builder()
                        .hasPermission(false)
                        .role(null)
                        .build());
    }
}

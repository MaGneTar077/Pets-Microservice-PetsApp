package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.RevokeAccessRequest;
import com.MyAnimaLog.Pets.application.dto.RevokeAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.RevokeAccessUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokeAccessService implements RevokeAccessUseCase {

    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final PetAccessPolicy petAccessPolicy;

    /** {@code request.ownerId} es obsoleto (PETS-02): revocar sigue siendo exclusivo del dueño. */
    @Override
    public RevokeAccessResponse execute(RevokeAccessRequest request) {
        petAccessPolicy.requireOwner(request.getPetId(), request.getOwnerId());

        petUserAccessRepositoryPort.findByPetIdAndUserId(request.getPetId(), request.getTargetUserId())
                .orElseThrow(AccessNotFoundException::new);

        petUserAccessRepositoryPort.deleteByPetIdAndUserId(request.getPetId(), request.getTargetUserId());

        return RevokeAccessResponse.builder()
                .message("Access revoked successfully")
                .build();
    }
}

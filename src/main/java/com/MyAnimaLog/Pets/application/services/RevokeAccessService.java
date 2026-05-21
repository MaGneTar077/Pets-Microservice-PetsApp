package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.RevokeAccessRequest;
import com.MyAnimaLog.Pets.application.dto.RevokeAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.RevokeAccessUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokeAccessService implements RevokeAccessUseCase {

    private  final PetRepositoryPort petRepositoryPort;
    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Override
    public RevokeAccessResponse execute(RevokeAccessRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        if (!pet.getOwnerId().equals(request.getOwnerId())) {
            throw new UnauthorizedPetAccessException();
        }

        petUserAccessRepositoryPort.findByPetIdAndUserId(request.getPetId(), request.getTargetUserId())
                .orElseThrow(AccessNotFoundException::new);

        petUserAccessRepositoryPort.deleteByPetIdAndUserId(request.getPetId(), request.getTargetUserId());

        return RevokeAccessResponse.builder()
                .message("Access revoked successfully")
                .build();
    }
}

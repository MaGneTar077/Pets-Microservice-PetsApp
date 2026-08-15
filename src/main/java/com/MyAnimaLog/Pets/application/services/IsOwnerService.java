package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsOwnerUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IsOwnerService implements IsOwnerUseCase {

    private final PetRepositoryPort petRepositoryPort;

    @Override
    public IsOwnerResponse execute(IsOwnerRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        boolean isOwner = pet.getOwnerId().equals(request.getUserId());

        return IsOwnerResponse.builder()
                .isOwner(isOwner)
                .build();
    }
}

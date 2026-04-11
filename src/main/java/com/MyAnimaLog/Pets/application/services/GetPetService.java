package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPetService implements GetPetUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetMapper petMapper;

    @Override
    public PetResponse getPetById(UUID petId) {
        Pet pet = petRepositoryPort.findById(petId)
                .orElseThrow(PetNotFoundException::new);

        return petMapper.toResponse(pet);
    }
}

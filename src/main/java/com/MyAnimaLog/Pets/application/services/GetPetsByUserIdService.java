package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsByUserIdUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPetsByUserIdService implements GetPetsByUserIdUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetMapper petMapper;

    @Override
    public List<PetResponse> getPetsByUserId(UUID userId) {
        List<Pet> pets = petRepositoryPort.findAllByOwnerId(userId);

        return pets.stream()
                .map(petMapper::toResponse)
                .toList();
    }
}

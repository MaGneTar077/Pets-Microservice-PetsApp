package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetUseCase;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPetService implements GetPetUseCase {

    private final PetMapper petMapper;
    private final PetAccessPolicy petAccessPolicy;

    /** Dueño, {@code EDITOR} o {@code VIEWER} pueden leer la mascota. */
    @Override
    public PetResponse getPetById(UUID petId) {
        Pet pet = petAccessPolicy.requireRead(petId);
        return petMapper.toResponse(pet);
    }
}

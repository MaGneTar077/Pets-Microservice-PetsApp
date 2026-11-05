package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsByUserIdUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
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
    private final CurrentUserIdentity currentUserIdentity;

    /**
     * Un usuario solo lista sus propias mascotas.
     *
     * @param userId {@code null} desde {@code GET /pets/me}; desde el alias obsoleto
     *               {@code GET /pets/user/{userId}} llega el valor de la ruta y solo se acepta si
     *               coincide con el token.
     */
    @Override
    public List<PetResponse> getPetsByUserId(UUID userId) {
        UUID ownerId = currentUserIdentity.resolveUserId(userId);

        List<Pet> pets = petRepositoryPort.findAllByOwnerId(ownerId);
        return pets.stream()
                .map(petMapper::toResponse)
                .toList();
    }
}

package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPetsBatchRequest;
import com.MyAnimaLog.Pets.application.dto.InternalPetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsBatchUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPetsBatchService implements GetPetsBatchUseCase {

    static final int MAX_IDS = 100;

    /** PETS-05 aún no existe en la tabla: ver {@link InternalPetResponse}. */
    private static final String DEFAULT_CLAIM_STATUS = "NONE";

    private final PetRepositoryPort petRepositoryPort;

    @Override
    public List<InternalPetResponse> execute(GetPetsBatchRequest request) {
        List<UUID> ids = request.getIds();

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAX_IDS) {
            throw new InvalidPetDataException("No se pueden consultar más de " + MAX_IDS + " mascotas por petición");
        }

        // Una sola consulta por clave primaria; las mascotas que no existan simplemente no vuelven.
        return petRepositoryPort.findAllByIds(ids).stream()
                .map(this::toResponse)
                .toList();
    }

    private InternalPetResponse toResponse(Pet pet) {
        return new InternalPetResponse(
                pet.getId(),
                pet.getOwnerId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getSex(),
                pet.getBirthDate(),
                pet.getPhotoUrl(),
                null,
                DEFAULT_CLAIM_STATUS,
                null);
    }
}

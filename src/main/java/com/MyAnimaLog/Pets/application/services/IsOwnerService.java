package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsOwnerUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Una sola consulta por clave primaria: una mascota inexistente devuelve {@code false}, nunca un
 * 404. Medical trata cualquier respuesta que no sea 2xx como servicio caído
 * (PETS_REQUERIMIENTOS_DESDE_MEDICAL.md §3).
 *
 * <p>PETS-05: cuando exista la columna {@code claim_status}, una mascota {@code UNCLAIMED} debe
 * devolver {@code false} aunque el {@code owner_id} coincida.
 */
@Service
@RequiredArgsConstructor
public class IsOwnerService implements IsOwnerUseCase {

    private final PetRepositoryPort petRepositoryPort;

    @Override
    public IsOwnerResponse execute(IsOwnerRequest request) {
        boolean isOwner = petRepositoryPort.existsByIdAndOwnerId(request.getPetId(), request.getUserId());

        return IsOwnerResponse.builder()
                .isOwner(isOwner)
                .build();
    }
}

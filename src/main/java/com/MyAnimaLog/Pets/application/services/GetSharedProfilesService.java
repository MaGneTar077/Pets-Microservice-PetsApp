package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSharedProfilesService implements GetSharedProfilesUseCase {

    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final CurrentUserIdentity currentUserIdentity;

    /**
     * Regla estricta de "lo mío" (PETS-02): un usuario solo ve las mascotas compartidas con él mismo.
     * {@code request.userId} llega {@code null} desde {@code GET /pets/shared/me}, o con el valor
     * de la ruta desde el alias obsoleto {@code GET /pets/shared?userId=}, aceptado solo si coincide
     * con el token.
     */
    @Override
    public List<SharedProfileResponse> execute(GetSharedProfilesRequest request) {
        UUID userId = currentUserIdentity.resolveUserId(request.getUserId());

        return petUserAccessRepositoryPort
                .findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SharedProfileResponse toResponse(PetUserAccess access) {
        return SharedProfileResponse.builder()
                .accessId(access.getId())
                .petId(access.getPetId())
                .userId(access.getUserId())
                .accessRole(access.getAccessRole())
                .createdAt(access.getCreatedAt())
                .build();
    }
}
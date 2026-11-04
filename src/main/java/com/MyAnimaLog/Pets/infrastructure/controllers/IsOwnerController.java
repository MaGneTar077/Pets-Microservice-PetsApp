package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsOwnerUseCase;
import com.MyAnimaLog.Pets.application.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @deprecated usar {@code GET /internal/pets/{petId}/owner/{userId}} desde otros servicios.
 * Este alias solo permite consultar la propia relación del usuario del token.
 */
@Deprecated
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class IsOwnerController {

    private final IsOwnerUseCase isOwnerUseCase;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Deprecated
    @GetMapping("/{petId}/owner/{userId}")
    public ResponseEntity<IsOwnerResponse> isOwner(
            @PathVariable UUID petId,
            @PathVariable UUID userId) {

        if (!userId.equals(authenticatedUserPort.current().userId())) {
            throw new UnauthorizedPetAccessException("You can only check your own access");
        }

        IsOwnerRequest request = IsOwnerRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        return ResponseEntity.ok(isOwnerUseCase.execute(request));
    }
}

package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.HasPermissionRequest;
import com.MyAnimaLog.Pets.application.dto.HasPermissionResponse;
import com.MyAnimaLog.Pets.application.ports.in.HasPermissionUseCase;
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
 * @deprecated pendiente de mover a {@code /internal/**}. Este alias solo permite consultar el
 * acceso del propio usuario del token.
 */
@Deprecated
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class HasPermissionController {

    private final HasPermissionUseCase hasPermissionUseCase;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Deprecated
    @GetMapping("/{petId}/access/{userId}")
    public ResponseEntity<HasPermissionResponse> hasPermission(
            @PathVariable UUID petId,
            @PathVariable UUID userId) {

        if (!userId.equals(authenticatedUserPort.current().userId())) {
            throw new UnauthorizedPetAccessException("You can only check your own access");
        }

        HasPermissionRequest request = HasPermissionRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        return ResponseEntity.ok(hasPermissionUseCase.execute(request));
    }
}

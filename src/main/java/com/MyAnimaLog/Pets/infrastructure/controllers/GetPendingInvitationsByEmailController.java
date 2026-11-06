package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByEmailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @deprecated usar {@code GET /pets/invitations/me}. Soporta las dos variantes históricas con las
 * que el frontend puede seguir enviando el correo: el header {@code X-User-Email} y el body
 * {@code { "email": "..." }}. Cualquiera de los dos solo se acepta si coincide con el email del
 * token (sin distinguir mayúsculas); si no coincide, 403 {@code IDENTITY_MISMATCH}.
 */
@Deprecated
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetPendingInvitationsByEmailController {

    private final GetPendingInvitationsByEmailUseCase getPendingInvitationsByEmailUseCase;

    @Deprecated
    @GetMapping("/invitations/pending")
    public ResponseEntity<List<PetInvitationResponse>> getPendingInvitationsByEmail(
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @RequestBody(required = false) GetPendingInvitationsByEmailRequest body) {

        String legacyEmail = headerEmail != null ? headerEmail : (body != null ? body.getEmail() : null);

        GetPendingInvitationsByEmailRequest request =
                GetPendingInvitationsByEmailRequest.builder()
                        .email(legacyEmail)
                        .build();

        return ResponseEntity.ok(getPendingInvitationsByEmailUseCase.execute(request));
    }
}

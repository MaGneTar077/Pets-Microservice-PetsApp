package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.AcceptInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated usar {@code POST /pets/invitations/{token}/accept}. Ruta que existía antes de que
 * las invitaciones se movieran bajo {@code /pets} (ver {@code AcceptInvitationController}).
 */
@Deprecated
@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class LegacyAcceptInvitationController {

    private final AcceptInvitationUseCase acceptInvitationUseCase;

    @Deprecated
    @PostMapping("/{token}/accept")
    public ResponseEntity<AcceptInvitationResponse> acceptInvitation(
            @PathVariable String token,
            @RequestBody(required = false) AcceptInvitationRequest request) {

        AcceptInvitationRequest fullRequest = (request != null ? request : new AcceptInvitationRequest())
                .toBuilder()
                .token(token)
                .build();

        return ResponseEntity.ok(acceptInvitationUseCase.execute(fullRequest));
    }
}

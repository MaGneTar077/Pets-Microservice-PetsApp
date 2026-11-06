package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByEmailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Las invitaciones pendientes para el email del token. Sustituye a {@code GET /pets/invitations/pending}. */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetMyPendingInvitationsController {

    private final GetPendingInvitationsByEmailUseCase getPendingInvitationsByEmailUseCase;

    @GetMapping("/invitations/me")
    public ResponseEntity<List<PetInvitationResponse>> getMyPendingInvitations() {
        GetPendingInvitationsByEmailRequest request = GetPendingInvitationsByEmailRequest.builder().build();
        return ResponseEntity.ok(getPendingInvitationsByEmailUseCase.execute(request));
    }
}

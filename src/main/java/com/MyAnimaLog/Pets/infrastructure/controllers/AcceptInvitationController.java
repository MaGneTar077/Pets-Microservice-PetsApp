package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.AcceptInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pets/invitations")
@RequiredArgsConstructor
public class AcceptInvitationController {

    private final AcceptInvitationUseCase acceptInvitationUseCase;

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
package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.ResendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.ResendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.ResendInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class ResendInvitationController {

    private final ResendInvitationUseCase resendInvitationUseCase;

    @PostMapping("/{petId}/invitations/resend")
    public ResponseEntity<ResendInvitationResponse> resendInvitation(
            @PathVariable UUID petId,
            @RequestBody ResendInvitationRequest request) {

        ResendInvitationRequest fullRequest = request.toBuilder()
                .petId(petId)
                .build();

        return ResponseEntity.ok(resendInvitationUseCase.execute(fullRequest));
    }
}
package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.SendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.SendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.SendInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class SendInvitationController {

    private final SendInvitationUseCase sendInvitationUseCase;

    @PostMapping("/{petId}/invitations")
    public ResponseEntity<SendInvitationResponse> sendInvitation(
            @PathVariable UUID petId,
            @RequestBody SendInvitationRequest request) {

        SendInvitationRequest fullRequest = request.toBuilder()
                .petId(petId)
                .build();

        return ResponseEntity.status(201).body(sendInvitationUseCase.execute(fullRequest));
    }
}
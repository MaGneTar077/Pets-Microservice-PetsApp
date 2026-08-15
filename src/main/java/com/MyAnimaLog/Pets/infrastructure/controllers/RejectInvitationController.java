package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.RejectInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.RejectInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class RejectInvitationController {

    private final RejectInvitationUseCase rejectInvitationUseCase;

    @PostMapping("/{token}/reject")
    public ResponseEntity<RejectInvitationResponse> rejectInvitation(
            @PathVariable String token) {

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token)
                .build();

        return ResponseEntity.ok(rejectInvitationUseCase.execute(request));
    }
}
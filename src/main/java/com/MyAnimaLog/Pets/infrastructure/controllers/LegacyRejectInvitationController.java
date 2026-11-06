package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.RejectInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.RejectInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated usar {@code POST /pets/invitations/{token}/reject}. Ruta que existía antes de que
 * las invitaciones se movieran bajo {@code /pets} (ver {@code RejectInvitationController}).
 */
@Deprecated
@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class LegacyRejectInvitationController {

    private final RejectInvitationUseCase rejectInvitationUseCase;

    @Deprecated
    @PostMapping("/{token}/reject")
    public ResponseEntity<RejectInvitationResponse> rejectInvitation(@PathVariable String token) {
        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token)
                .build();

        return ResponseEntity.ok(rejectInvitationUseCase.execute(request));
    }
}

package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByEmailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetPendingInvitationsByEmailController {

    private final GetPendingInvitationsByEmailUseCase getPendingInvitationsByEmailUseCase;

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<PetInvitationResponse>> getPendingInvitationsByEmail(
            @RequestBody GetPendingInvitationsByEmailRequest request) {

        return ResponseEntity.ok(getPendingInvitationsByEmailUseCase.execute(request));
    }
}
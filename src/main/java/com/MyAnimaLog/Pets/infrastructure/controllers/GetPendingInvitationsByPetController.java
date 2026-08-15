package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByPetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetPendingInvitationsByPetController {

    private final GetPendingInvitationsByPetUseCase getPendingInvitationsByPetUseCase;

    @GetMapping("/{petId}/invitations/pending")
    public ResponseEntity<List<PetInvitationResponse>> getPendingInvitations(
            @PathVariable UUID petId,
            @RequestParam UUID ownerId) {

        GetPendingInvitationsByPetRequest request = GetPendingInvitationsByPetRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .build();

        return ResponseEntity.ok(getPendingInvitationsByPetUseCase.execute(request));
    }
}
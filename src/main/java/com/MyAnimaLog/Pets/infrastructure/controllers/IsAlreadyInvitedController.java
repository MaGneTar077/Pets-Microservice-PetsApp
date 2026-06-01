package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedRequest;
import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsAlreadyInvitedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class IsAlreadyInvitedController {

    private final IsAlreadyInvitedUseCase isAlreadyInvitedUseCase;

    @GetMapping("/{petId}/invitations/check")
    public ResponseEntity<IsAlreadyInvitedResponse> isAlreadyInvited(
            @PathVariable UUID petId,
            @RequestParam String email) {

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId)
                .email(email)
                .build();

        return ResponseEntity.ok(isAlreadyInvitedUseCase.execute(request));
    }
}
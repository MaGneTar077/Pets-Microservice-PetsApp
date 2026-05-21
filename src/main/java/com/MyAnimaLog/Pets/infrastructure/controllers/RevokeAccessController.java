package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.RevokeAccessRequest;
import com.MyAnimaLog.Pets.application.dto.RevokeAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.RevokeAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class RevokeAccessController {

    private final RevokeAccessUseCase revokeAccessUseCase;

    @DeleteMapping("/{petId}/access/{targetUserId}")
    public ResponseEntity<RevokeAccessResponse> revokeAccess(
            @PathVariable UUID petId,
            @PathVariable UUID targetUserId,
            @RequestParam UUID ownerId) {

        RevokeAccessRequest request = RevokeAccessRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .targetUserId(targetUserId)
                .build();

        return ResponseEntity.ok(revokeAccessUseCase.execute(request));
    }
}

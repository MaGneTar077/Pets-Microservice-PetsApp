package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GrantAccessRequest;
import com.MyAnimaLog.Pets.application.dto.GrantAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.GrantAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GrantAccessController {

    private final GrantAccessUseCase grantAccessUseCase;

    @PostMapping("/{petId}/access")
    public ResponseEntity<GrantAccessResponse> grantAccess(
            @PathVariable UUID petId,
            @RequestBody GrantAccessRequest request) {

        GrantAccessRequest fullRequest = request.toBuilder()
                .petId(petId)
                .build();

        return ResponseEntity.status(201).body(grantAccessUseCase.execute(fullRequest));
    }
}
package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.HasPermissionRequest;
import com.MyAnimaLog.Pets.application.dto.HasPermissionResponse;
import com.MyAnimaLog.Pets.application.ports.in.HasPermissionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class HasPermissionController {

    private final HasPermissionUseCase hasPermissionUseCase;

    @GetMapping("/{petId}/access/{userId}")
    public ResponseEntity<HasPermissionResponse> hasPermission(
            @PathVariable UUID petId,
            @PathVariable UUID userId) {

        HasPermissionRequest request = HasPermissionRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        return ResponseEntity.ok(hasPermissionUseCase.execute(request));
    }
}

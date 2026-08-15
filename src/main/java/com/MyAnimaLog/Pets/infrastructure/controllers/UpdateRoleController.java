package com.MyAnimaLog.Pets.infrastructure.controllers;


import com.MyAnimaLog.Pets.application.dto.UpdateRoleRequest;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleResponse;
import com.MyAnimaLog.Pets.application.ports.in.UpdateRoleUseCase;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class UpdateRoleController {

    private final UpdateRoleUseCase updateRoleUseCase;

    @PatchMapping("/{petId}/access/{targetUserId}")
    public ResponseEntity<UpdateRoleResponse> updateRole(
            @PathVariable UUID petId,
            @PathVariable UUID targetUserId,
            @RequestBody UpdateRoleRequest request) {

        UpdateRoleRequest fullRequest = request.toBuilder()
                .petId(petId)
                .targetUserId(targetUserId)
                .build();

        return ResponseEntity.ok(updateRoleUseCase.execute(fullRequest));
    }
}

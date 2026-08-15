package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesByPetRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesByPetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetSharedProfilesByPetController {

    private final GetSharedProfilesByPetUseCase getSharedProfilesByPetUseCase;

    @GetMapping("/{petId}/shared")
    public ResponseEntity<List<SharedProfileResponse>> getSharedProfilesByPet(
            @PathVariable UUID petId,
            @RequestParam UUID ownerId) {

        GetSharedProfilesByPetRequest request = GetSharedProfilesByPetRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .build();

        return ResponseEntity.ok(getSharedProfilesByPetUseCase.execute(request));
    }
}
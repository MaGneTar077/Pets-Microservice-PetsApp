package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.ports.in.DeletePetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class DeletePetController {

    private final DeletePetUseCase deletePetUseCase;

    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(
            @PathVariable UUID petId,
            @RequestParam UUID ownerId) {
        deletePetUseCase.deletePet(petId, ownerId);
        return ResponseEntity.noContent().build();
    }
}

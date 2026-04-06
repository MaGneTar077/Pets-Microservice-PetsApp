package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.EditPetRequest;
import com.MyAnimaLog.Pets.application.dto.EditPetResponse;
import com.MyAnimaLog.Pets.application.ports.in.EditPetUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class EditPetController {
    private final EditPetUseCase editPetUseCase;

    @PatchMapping("/{petId}")
    public ResponseEntity<EditPetResponse> editPet(
            @PathVariable UUID petId,
            @RequestParam UUID ownerId,
            @Valid @RequestBody EditPetRequest request
    ) {

        EditPetResponse response = editPetUseCase.editPet(petId, ownerId, request);

        return ResponseEntity.ok(response);
    }
}

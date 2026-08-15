package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetUseCase;
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
public class GetPetController {

    private final GetPetUseCase getPetUseCase;

    @GetMapping("/{petId}")
    public ResponseEntity<PetResponse> getPetsById(@PathVariable UUID petId) {
        return ResponseEntity.ok(getPetUseCase.getPetById(petId));
    }

}

package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsByUserIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetPetsByUserIdController {

    private final GetPetsByUserIdUseCase getPetsByUserIdUseCase;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PetResponse>> getPetsByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(getPetsByUserIdUseCase.getPetsByUserId(userId));
    }
}

package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsByUserIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Las mascotas del usuario del token. Sustituye a {@code GET /pets/user/{userId}}. */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetMyPetsController {

    private final GetPetsByUserIdUseCase getPetsByUserIdUseCase;

    @GetMapping("/me")
    public ResponseEntity<List<PetResponse>> getMyPets() {
        return ResponseEntity.ok(getPetsByUserIdUseCase.getPetsByUserId(null));
    }
}

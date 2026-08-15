package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AddPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.services.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class AddPetController {
    private final PetService petService;

    @PostMapping
    public ResponseEntity<PetResponse> addPet(@Valid @RequestBody AddPetRequest request) {
        PetResponse response = petService.add(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

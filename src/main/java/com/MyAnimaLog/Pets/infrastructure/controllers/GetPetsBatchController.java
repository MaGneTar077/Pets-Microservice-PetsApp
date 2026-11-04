package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPetsBatchRequest;
import com.MyAnimaLog.Pets.application.dto.InternalPetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsBatchUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo para servicios internos: autenticado con {@code X-Internal-Api-Key}, no con JWT. */
@RestController
@RequestMapping("/internal/pets")
@RequiredArgsConstructor
public class GetPetsBatchController {

    private final GetPetsBatchUseCase getPetsBatchUseCase;

    @PostMapping("/batch")
    public ResponseEntity<List<InternalPetResponse>> batch(@RequestBody GetPetsBatchRequest request) {
        return ResponseEntity.ok(getPetsBatchUseCase.execute(request));
    }
}

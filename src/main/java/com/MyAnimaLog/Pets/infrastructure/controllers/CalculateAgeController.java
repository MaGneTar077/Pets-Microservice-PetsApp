package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AgeResponse;
import com.MyAnimaLog.Pets.application.ports.in.CalculateAgeUseCase;
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
public class CalculateAgeController {

    private final CalculateAgeUseCase calculateAgeUseCase;

    @GetMapping("/{petId}/age")
    public ResponseEntity<AgeResponse> calculateAge(@PathVariable UUID petId) {
        return ResponseEntity.ok(calculateAgeUseCase.calculateAge(petId));
    }
}

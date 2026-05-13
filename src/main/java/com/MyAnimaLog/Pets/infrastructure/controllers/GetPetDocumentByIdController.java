package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetDocumentByIdUseCase;
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
public class GetPetDocumentByIdController {

    private final GetPetDocumentByIdUseCase getPetDocumentByIdUseCase;

    @GetMapping("/{petId}/documents/{documentId}")
    public ResponseEntity<GetPetDocumentResponse> getDocumentById(
            @PathVariable UUID petId,
            @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(getPetDocumentByIdUseCase.execute(petId, documentId));
    }
}

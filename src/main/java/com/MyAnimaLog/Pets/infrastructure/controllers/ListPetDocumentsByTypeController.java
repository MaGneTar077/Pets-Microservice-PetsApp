package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.application.ports.in.ListPetDocumentsByTypeUseCase;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
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
public class ListPetDocumentsByTypeController {

    private final ListPetDocumentsByTypeUseCase listPetDocumentsByTypeUseCase;

    @GetMapping("/{petId}/documents/type/{documentType}")
    public ResponseEntity<List<PetDocumentSummaryResponse>> listDocumentsByType(
            @PathVariable UUID petId,
            @PathVariable DocumentType documentType
    ) {
        return ResponseEntity.ok(listPetDocumentsByTypeUseCase.execute(petId, documentType));
    }
}

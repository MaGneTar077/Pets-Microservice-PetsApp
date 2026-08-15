package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.application.ports.in.ListPetDocumentsUseCase;
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
public class ListPetDocumentsController {

    private final ListPetDocumentsUseCase listPetDocumentsUseCase;

    @GetMapping("/{petId}/documents")
    public ResponseEntity<List<PetDocumentSummaryResponse>> listDocuments(
            @PathVariable UUID petId
    ) {
        return ResponseEntity.ok(listPetDocumentsUseCase.execute(petId));
    }
}

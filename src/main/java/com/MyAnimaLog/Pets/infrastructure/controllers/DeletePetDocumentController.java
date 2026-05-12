package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class DeletePetDocumentController {

    private final DeletePetDocumentUseCase deletePetDocumentUseCase;

    @DeleteMapping("/{petId}/documents/{documentId}")
    public ResponseEntity<DeletePetDocumentResponse> deleteDocument(
            @PathVariable UUID petId,
            @PathVariable UUID documentId,
            @RequestParam UUID requestedBy
    ) {
        DeletePetDocumentRequest request = DeletePetDocumentRequest.builder()
                .petId(petId)
                .documentId(documentId)
                .requestedBy(requestedBy)
                .build();

        return ResponseEntity.ok(deletePetDocumentUseCase.execute(request));
    }
}

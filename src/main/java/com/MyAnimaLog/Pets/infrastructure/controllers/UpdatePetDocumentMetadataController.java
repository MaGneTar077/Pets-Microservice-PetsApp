package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataRequest;
import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataResponse;
import com.MyAnimaLog.Pets.application.ports.in.UpdatePetDocumentMetadataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class UpdatePetDocumentMetadataController {

    private final UpdatePetDocumentMetadataUseCase updatePetDocumentMetadataUseCase;

    @PatchMapping("/{petId}/documents/{documentId}/metadata")
    public ResponseEntity<UpdatePetDocumentMetadataResponse> updateMetadata(
            @PathVariable UUID petId,
            @PathVariable UUID documentId,
            @RequestBody UpdatePetDocumentMetadataRequest request
    ) {

        request.setPetId(petId);
        request.setDocumentId(documentId);

        return ResponseEntity.ok(
                updatePetDocumentMetadataUseCase.execute(request)
        );
    }
}

package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.UploadPetDocumentUseCase;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class UploadPetDocumentController {

    private final UploadPetDocumentUseCase uploadPetDocumentUseCase;

    @PostMapping(
            value = "/{petId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadPetDocumentResponse> uploadDocument(
            @PathVariable UUID petId,
            @RequestParam UUID uploadedBy,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam DocumentType documentType,
            @RequestPart("file") MultipartFile file
    ) {
        UploadPetDocumentRequest request = UploadPetDocumentRequest.builder()
                .petId(petId)
                .uploadedBy(uploadedBy)
                .title(title)
                .description(description)
                .documentType(documentType)
                .file(file)
                .build();

        return ResponseEntity.status(201).body(uploadPetDocumentUseCase.execute(request));
    }
}

package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.in.UploadPetPhotoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetPhotoController {
    private final UploadPetPhotoUseCase uploadPetPhotoUseCase;

    @PatchMapping(
            value = "/{petId}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadPetPhotoResponse> uploadPhoto(
            @PathVariable UUID petId,
            @RequestParam UUID ownerId,
            @RequestPart("file") MultipartFile file
    ) {
        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .file(file)
                .build();

        return ResponseEntity.ok(uploadPetPhotoUseCase.execute(request));
    }
}


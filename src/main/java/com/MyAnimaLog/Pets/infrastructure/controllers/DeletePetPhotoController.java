package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetPhotoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class DeletePetPhotoController {

    private final DeletePetPhotoUseCase deletePetPhotoUseCase;

    @DeleteMapping("/{petId}/photo")
    public ResponseEntity<DeletePetPhotoResponse> deletePhoto(
            @PathVariable UUID petId,
            @RequestParam UUID ownerId
    ) {
        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .build();

        return ResponseEntity.ok(deletePetPhotoUseCase.execute(request));
    }
}

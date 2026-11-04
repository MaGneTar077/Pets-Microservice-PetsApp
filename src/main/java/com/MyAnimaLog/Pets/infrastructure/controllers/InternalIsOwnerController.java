package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.InternalIsOwnerResponse;
import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.ports.in.IsOwnerUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Solo para servicios internos: autenticado con {@code X-Internal-Api-Key}, no con JWT. */
@RestController
@RequestMapping("/internal/pets")
@RequiredArgsConstructor
public class InternalIsOwnerController {

    private final IsOwnerUseCase isOwnerUseCase;

    @GetMapping("/{petId}/owner/{userId}")
    public ResponseEntity<InternalIsOwnerResponse> isOwner(
            @PathVariable UUID petId,
            @PathVariable UUID userId) {

        IsOwnerRequest request = IsOwnerRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        return ResponseEntity.ok(new InternalIsOwnerResponse(isOwnerUseCase.execute(request).isOwner()));
    }
}

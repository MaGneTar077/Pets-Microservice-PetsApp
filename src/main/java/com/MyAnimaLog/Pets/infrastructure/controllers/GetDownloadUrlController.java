package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlRequest;
import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetDownloadUrlUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetDownloadUrlController {

    private final GetDownloadUrlUseCase getDownloadUrlUseCase;

    @GetMapping("/{petId}/documents/{documentId}/download-url")
    public ResponseEntity<GetDownloadUrlResponse> getDownloadUrl(
            @PathVariable UUID petId,
            @PathVariable UUID documentId,
            @RequestParam UUID requestedBy) {

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId)
                .documentId(documentId)
                .requestedBy(requestedBy)
                .build();

        return ResponseEntity.ok(getDownloadUrlUseCase.getDownloadUrl(request));
    }
}

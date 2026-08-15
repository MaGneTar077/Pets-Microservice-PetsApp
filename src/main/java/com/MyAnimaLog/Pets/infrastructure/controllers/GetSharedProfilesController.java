package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetSharedProfilesController {

    private final GetSharedProfilesUseCase getSharedProfilesUseCase;

    @GetMapping("/shared")
    public ResponseEntity<List<SharedProfileResponse>> getSharedProfiles(
            @RequestParam UUID userId) {

        GetSharedProfilesRequest request = GetSharedProfilesRequest.builder()
                .userId(userId)
                .build();

        return ResponseEntity.ok(getSharedProfilesUseCase.execute(request));
    }
}
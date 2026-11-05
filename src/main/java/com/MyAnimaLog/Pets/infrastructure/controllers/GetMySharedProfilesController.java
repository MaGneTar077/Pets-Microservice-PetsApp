package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Las mascotas compartidas con el usuario del token. Sustituye a {@code GET /pets/shared?userId=}. */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class GetMySharedProfilesController {

    private final GetSharedProfilesUseCase getSharedProfilesUseCase;

    @GetMapping("/shared/me")
    public ResponseEntity<List<SharedProfileResponse>> getMySharedProfiles() {
        GetSharedProfilesRequest request = GetSharedProfilesRequest.builder().build();
        return ResponseEntity.ok(getSharedProfilesUseCase.execute(request));
    }
}

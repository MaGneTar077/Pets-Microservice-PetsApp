package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.IsExpiredRequest;
import com.MyAnimaLog.Pets.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsExpiredUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class IsExpiredController {

    private final IsExpiredUseCase isExpiredUseCase;

    @GetMapping("/invitations/{token}/expired")
    public ResponseEntity<IsExpiredResponse> isExpired(
            @PathVariable String token) {

        IsExpiredRequest request = IsExpiredRequest.builder()
                .token(token)
                .build();

        return ResponseEntity.ok(isExpiredUseCase.execute(request));
    }
}

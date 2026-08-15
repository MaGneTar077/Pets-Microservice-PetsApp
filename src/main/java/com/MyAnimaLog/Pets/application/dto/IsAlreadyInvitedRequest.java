package com.MyAnimaLog.Pets.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IsAlreadyInvitedRequest {
    private UUID petId;
    private String email;
}
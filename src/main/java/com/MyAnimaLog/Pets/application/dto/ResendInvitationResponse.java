package com.MyAnimaLog.Pets.application.dto;

import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResendInvitationResponse {
    private UUID id;
    private UUID petId;
    private String email;
    private Rol accessRole;
    private InvitationStatus status;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
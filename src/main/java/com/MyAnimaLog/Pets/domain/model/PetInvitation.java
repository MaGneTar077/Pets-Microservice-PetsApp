package com.MyAnimaLog.Pets.domain.model;

import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class PetInvitation {
    private UUID id;
    private UUID petId;
    private UUID userId;
    private String email;
    private Rol accessRole;
    private InvitationStatus status;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

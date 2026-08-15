package com.MyAnimaLog.Pets.application.dto;

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
public class UpdateRoleResponse {
    private UUID id;
    private UUID petId;
    private UUID userId;
    private Rol newRole;
    private LocalDateTime updatedAt;
}

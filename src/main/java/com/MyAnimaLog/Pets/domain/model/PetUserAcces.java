package com.MyAnimaLog.Pets.domain.model;

import com.MyAnimaLog.Pets.domain.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class PetUserAcces {
    private UUID id;
    private UUID petId;
    private UUID userId;
    private Rol accesRole;
    private LocalDateTime createdAt;
}

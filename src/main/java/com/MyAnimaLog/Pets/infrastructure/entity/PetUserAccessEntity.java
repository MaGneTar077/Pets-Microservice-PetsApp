package com.MyAnimaLog.Pets.infrastructure.entity;

import com.MyAnimaLog.Pets.domain.enums.Rol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "pet_user_access")
public class PetUserAccessEntity {

    @Id
    private UUID id;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role", nullable = false)
    private Rol accessRole;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

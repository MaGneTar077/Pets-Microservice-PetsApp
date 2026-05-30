package com.MyAnimaLog.Pets.infrastructure.mapper;

import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import com.MyAnimaLog.Pets.infrastructure.entity.PetInvitationEntity;
import org.springframework.stereotype.Component;

@Component
public class PetInvitationMapper {
    public PetInvitationEntity toEntity(PetInvitation domain) {
        return PetInvitationEntity.builder()
                .id(domain.getId())
                .petId(domain.getPetId())
                .userId(domain.getUserId())
                .email(domain.getEmail())
                .accessRole(domain.getAccessRole())
                .status(domain.getStatus())
                .token(domain.getToken())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public PetInvitation toDomain(PetInvitationEntity entity) {
        return PetInvitation.builder()
                .id(entity.getId())
                .petId(entity.getPetId())
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .accessRole(entity.getAccessRole())
                .status(entity.getStatus())
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

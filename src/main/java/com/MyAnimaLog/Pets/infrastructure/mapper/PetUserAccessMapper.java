package com.MyAnimaLog.Pets.infrastructure.mapper;

import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import com.MyAnimaLog.Pets.infrastructure.entity.PetUserAccessEntity;
import org.springframework.stereotype.Component;

@Component
public class PetUserAccessMapper {

    public PetUserAccessEntity toEntity(PetUserAccess domain) {
        return PetUserAccessEntity.builder()
                .id(domain.getId())
                .petId(domain.getPetId())
                .userId(domain.getUserId())
                .accessRole(domain.getAccessRole())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public PetUserAccess toDomain(PetUserAccessEntity entity) {
        return PetUserAccess.builder()
                .id(entity.getId())
                .petId(entity.getPetId())
                .userId(entity.getUserId())
                .accessRole(entity.getAccessRole())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

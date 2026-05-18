package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetUserAccessMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetUserAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PetUserAccessRepositoryAdapter implements PetUserAccessRepositoryPort {

    private final JpaPetUserAccessRepository jpaPetUserAccessRepository;
    private final PetUserAccessMapper petUserAccessMapper;

    @Override
    public PetUserAccess save(PetUserAccess petUserAccess) {
        return petUserAccessMapper.toDomain(
                jpaPetUserAccessRepository.save(petUserAccessMapper.toEntity(petUserAccess))
        );
    }

    @Override
    public Optional<PetUserAccess> findByPetIdAndUserId(UUID petId, UUID userId) {
        return jpaPetUserAccessRepository.findByPetIdAndUserId(petId, userId)
                .map(petUserAccessMapper::toDomain);
    }

    @Override
    public void deleteByPetIdAndUserId(UUID petId, UUID userId) {
        jpaPetUserAccessRepository.deleteByPetIdAndUserId(petId, userId);
    }
}
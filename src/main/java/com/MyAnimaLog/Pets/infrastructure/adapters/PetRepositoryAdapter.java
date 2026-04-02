package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetRepositoryAdapter implements PetRepositoryPort {

    private final JpaPetRepository jpaPetRepository;
    private final PetMapper petMapper;

    @Override
    public Pet save(Pet pet) {
        return petMapper.toDomain(
                jpaPetRepository.save(
                        petMapper.toEntity(pet)
                )
        );
    }
}
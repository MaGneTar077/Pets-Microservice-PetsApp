package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<Pet> findById(UUID id) {
        return jpaPetRepository.findById(id)
                .map(petMapper::toDomain);
    }

    @Override
    public Optional<Pet> findByIdAndOwnerId(UUID petId, UUID ownerId) {
        return jpaPetRepository.findById(petId)
                .filter(p -> p.getOwnerId().equals(ownerId))
                .map(petMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaPetRepository.deleteById(id);
    }

    @Override
    public List<Pet> findAllByOwnerId(UUID ownerId) {
        return jpaPetRepository.findAllByOwnerId(ownerId).stream()
                .map(petMapper::toDomain)
                .toList();
    }
}
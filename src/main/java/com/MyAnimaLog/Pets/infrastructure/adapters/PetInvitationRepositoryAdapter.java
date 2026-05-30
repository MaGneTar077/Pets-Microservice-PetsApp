package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetInvitationMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PetInvitationRepositoryAdapter implements PetInvitationRepositoryPort {

    private final JpaPetInvitationRepository jpaPetInvitationRepository;
    private final PetInvitationMapper petInvitationMapper;

    @Override
    public PetInvitation save(PetInvitation invitation) {
        return petInvitationMapper.toDomain(
                jpaPetInvitationRepository.save(petInvitationMapper.toEntity(invitation))
        );
    }

    @Override
    public Optional<PetInvitation> findById(UUID id) {
        return jpaPetInvitationRepository.findById(id)
                .map(petInvitationMapper::toDomain);
    }

    @Override
    public Optional<PetInvitation> findByPetIdAndEmailAndStatus(UUID petId, String email, InvitationStatus status) {
        return jpaPetInvitationRepository.findByPetIdAndEmailAndStatus(petId, email, status)
                .map(petInvitationMapper::toDomain);
    }

    @Override
    public List<PetInvitation> findAllByPetIdAndStatus(UUID petId, InvitationStatus status) {
        return jpaPetInvitationRepository.findAllByPetIdAndStatus(petId, status)
                .stream()
                .map(petInvitationMapper::toDomain)
                .toList();
    }

    @Override
    public List<PetInvitation> findAllByEmailAndStatus(String email, InvitationStatus status) {
        return jpaPetInvitationRepository.findAllByEmailAndStatus(email, status)
                .stream()
                .map(petInvitationMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PetInvitation> findByToken(String token) {
        return jpaPetInvitationRepository.findByToken(token)
                .map(petInvitationMapper::toDomain);
    }
}
package com.MyAnimaLog.Pets.infrastructure.repositories;

import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.infrastructure.entity.PetInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPetInvitationRepository extends JpaRepository<PetInvitationEntity, UUID> {
    Optional<PetInvitationEntity> findByPetIdAndEmailAndStatus(UUID petId, String email, InvitationStatus status);
    List<PetInvitationEntity> findAllByPetIdAndStatus(UUID petId, InvitationStatus status);
    List<PetInvitationEntity> findAllByEmailAndStatus(String email, InvitationStatus status);
    Optional<PetInvitationEntity> findByToken(String token);
    List<PetInvitationEntity> findAllByPetIdAndEmail(UUID petId, String email);
}

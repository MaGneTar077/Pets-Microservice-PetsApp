package com.MyAnimaLog.Pets.application.ports.out;

import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetInvitationRepositoryPort {
    PetInvitation save(PetInvitation invitation);
    Optional<PetInvitation> findById(UUID id);
    Optional<PetInvitation> findByPetIdAndEmailAndStatus(UUID petId, String email, InvitationStatus status);
    List<PetInvitation> findAllByPetIdAndStatus(UUID petId, InvitationStatus status);
    List<PetInvitation> findAllByEmailAndStatus(String email, InvitationStatus status);
    Optional<PetInvitation> findByToken(String token);
    List<PetInvitation> findAllByPetIdAndEmail(UUID petId, String email);
}

package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.in.RejectInvitationUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RejectInvitationService implements RejectInvitationUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;
    private final PetRepositoryPort petRepositoryPort;
    private final PublishPetEventUseCase publishPetEventUseCase;


    @Override
    public RejectInvitationResponse execute(RejectInvitationRequest request) {
        PetInvitation invitation = petInvitationRepositoryPort
                .findByToken(request.getToken())
                .orElseThrow(InvitationNotFoundException::new);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationAlreadyProcessedException();
        }

        PetInvitation updated = invitation.toBuilder()
                .status(InvitationStatus.REJECTED)
                .build();

        petInvitationRepositoryPort.save(updated);

        Pet pet = petRepositoryPort.findById(invitation.getPetId())
                .orElseThrow(() -> new PetNotFoundException(invitation.getPetId()));

        publishPetEventUseCase.publish(PetEvent.builder()
                .petId(pet.getId())
                .ownerId(pet.getOwnerId())
                .petName(pet.getName())
                .eventType("PET_INVITATION_REJECTED")
                .occurredAt(Instant.now())
                .build());

        return RejectInvitationResponse.builder()
                .invitationId(invitation.getId())
                .petId(invitation.getPetId())
                .status(InvitationStatus.REJECTED)
                .build();
    }
}

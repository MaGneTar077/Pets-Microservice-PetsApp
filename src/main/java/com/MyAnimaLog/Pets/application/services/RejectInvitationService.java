package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.RejectInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.RejectInvitationUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RejectInvitationService implements RejectInvitationUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;


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

        return RejectInvitationResponse.builder()
                .invitationId(invitation.getId())
                .petId(invitation.getPetId())
                .status(InvitationStatus.REJECTED)
                .build();
    }
}

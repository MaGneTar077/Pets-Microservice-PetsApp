package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByEmailUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPendingInvitationsByEmailService implements GetPendingInvitationsByEmailUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Override
    public List<PetInvitationResponse> execute(GetPendingInvitationsByEmailRequest request) {
        return petInvitationRepositoryPort
                .findAllByEmailAndStatus(request.getEmail(), InvitationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PetInvitationResponse toResponse(PetInvitation invitation) {
        return PetInvitationResponse.builder()
                .id(invitation.getId())
                .petId(invitation.getPetId())
                .email(invitation.getEmail())
                .accessRole(invitation.getAccessRole())
                .status(invitation.getStatus())
                .token(invitation.getToken())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .build();

    }
}

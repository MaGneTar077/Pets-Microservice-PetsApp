package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByPetUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPendingInvitationsByPetService implements GetPendingInvitationsByPetUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Override
    public List<PetInvitationResponse> execute(GetPendingInvitationsByPetRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        if (!pet.getOwnerId().equals(request.getOwnerId())) {
            throw new UnauthorizedPetAccessException();
        }

        return petInvitationRepositoryPort
                .findAllByPetIdAndStatus(request.getPetId(), InvitationStatus.PENDING)
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

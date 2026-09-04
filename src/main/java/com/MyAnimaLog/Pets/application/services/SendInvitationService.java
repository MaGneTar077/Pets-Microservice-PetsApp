package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.dto.SendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.SendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.in.SendInvitationUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyExistsException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendInvitationService implements SendInvitationUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetInvitationRepositoryPort petInvitationRepositoryPort;
    private final PublishPetEventUseCase publishPetEventUseCase;

    @Override
    public SendInvitationResponse execute(SendInvitationRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        if (!pet.getOwnerId().equals(request.getOwnerId())) {
            throw new UnauthorizedPetAccessException();
        }

        petInvitationRepositoryPort
                .findByPetIdAndEmailAndStatus(request.getPetId(), request.getEmail(), InvitationStatus.PENDING)
                .ifPresent(existing -> { throw new InvitationAlreadyExistsException(); });

        PetInvitation invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(request.getPetId())
                .email(request.getEmail())
                .accessRole(request.getAccessRole())
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        PetInvitation saved = petInvitationRepositoryPort.save(invitation);

        publishPetEventUseCase.publish(PetEvent.builder()
                .petId(pet.getId())
                .ownerId(pet.getOwnerId())
                .petName(pet.getName())
                .eventType("PET_INVITATION_SENT")
                .occurredAt(Instant.now())
                .build());

        return SendInvitationResponse.builder()
                .id(saved.getId())
                .petId(saved.getPetId())
                .email(saved.getEmail())
                .accessRole(saved.getAccessRole())
                .status(saved.getStatus())
                .token(saved.getToken())
                .expiresAt(saved.getExpiresAt())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}

package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.dto.ResendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.ResendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.in.ResendInvitationUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResendInvitationService implements ResendInvitationUseCase {

    private final PetRepositoryPort petRepositoryPort;
    private final PetInvitationRepositoryPort petInvitationRepositoryPort;
    private final PublishPetEventUseCase publishPetEventUseCase;


    @Override
    public ResendInvitationResponse execute(ResendInvitationRequest request) {
        Pet pet = petRepositoryPort.findById(request.getPetId())
                .orElseThrow(() -> new PetNotFoundException(request.getPetId()));

        if (!pet.getOwnerId().equals(request.getOwnerId())) {
            throw new UnauthorizedPetAccessException();
        }

        List<PetInvitation> existing = petInvitationRepositoryPort
                .findAllByPetIdAndEmail(request.getPetId(), request.getEmail());

        if (existing.isEmpty()) {
            throw new InvitationNotFoundException("No invitation found for this email and pet");
        }

        existing.forEach(inv -> {
            PetInvitation cancelled = inv.toBuilder()
                    .status(InvitationStatus.CANCELLED)
                    .build();
            petInvitationRepositoryPort.save(cancelled);
        });

        PetInvitation newInvitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(request.getPetId())
                .email(request.getEmail())
                .accessRole(request.getAccessRole())
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        PetInvitation saved = petInvitationRepositoryPort.save(newInvitation);

        publishPetEventUseCase.publish(PetEvent.builder()
                .petId(pet.getId())
                .ownerId(pet.getOwnerId())
                .petName(pet.getName())
                .eventType("PET_INVITATION_RESENT")
                .occurredAt(Instant.now())
                .build());

        return ResendInvitationResponse.builder()
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

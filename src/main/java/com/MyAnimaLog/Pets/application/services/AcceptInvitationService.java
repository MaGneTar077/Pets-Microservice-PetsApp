package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Pets.application.dto.AuthenticatedUser;
import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.ports.in.AcceptInvitationUseCase;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.exceptions.AccessAlreadyGrantedException;
import com.MyAnimaLog.Pets.domain.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptInvitationService implements AcceptInvitationUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;
    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final PetRepositoryPort petRepositoryPort;
    private final PublishPetEventUseCase publishPetEventUseCase;
    private final CurrentUserIdentity currentUserIdentity;

    /**
     * {@code request.userId} es obsoleto (PETS-02): quien acepta es el {@code sub} del token.
     * Además, el email del token debe coincidir con el de la invitación (sin distinguir
     * mayúsculas) y debe estar verificado — este es el hueco que PETS-02 cierra: antes solo se
     * validaba el token de la invitación, así que cualquier usuario autenticado con el enlace
     * podía aceptar la invitación de otra persona.
     */
    @Override
    public AcceptInvitationResponse execute(AcceptInvitationRequest request) {
        UUID userId = currentUserIdentity.resolveUserId(request.getUserId());

        PetInvitation invitation = petInvitationRepositoryPort
                .findByToken(request.getToken())
                .orElseThrow(InvitationNotFoundException::new);

        validateInvitationEmail(invitation);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationAlreadyProcessedException();
        }

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            throw new InvitationAlreadyProcessedException("This invitation has expired");
        }

        petUserAccessRepositoryPort.findByPetIdAndUserId(invitation.getPetId(), userId)
                .ifPresent(existing -> { throw new AccessAlreadyGrantedException(); });

        PetInvitation updated = invitation.toBuilder()
                .status(InvitationStatus.ACCEPTED)
                .userId(userId)
                .build();

        petInvitationRepositoryPort.save(updated);

        PetUserAccess access = PetUserAccess.builder()
                .id(UUID.randomUUID())
                .petId(invitation.getPetId())
                .userId(userId)
                .accessRole(invitation.getAccessRole())
                .createdAt(LocalDateTime.now())
                .build();

        petUserAccessRepositoryPort.save(access);

        Pet pet = petRepositoryPort.findById(invitation.getPetId())
                .orElseThrow(() -> new PetNotFoundException(invitation.getPetId()));

        publishPetEventUseCase.publish(PetEvent.builder()
                .petId(pet.getId())
                .ownerId(pet.getOwnerId())
                .petName(pet.getName())
                .eventType("PET_INVITATION_ACCEPTED")
                .occurredAt(Instant.now())
                .build());

        return AcceptInvitationResponse.builder()
                .invitationId(invitation.getId())
                .petId(invitation.getPetId())
                .userId(userId)
                .accessRole(invitation.getAccessRole())
                .status(InvitationStatus.ACCEPTED)
                .build();
    }

    private void validateInvitationEmail(PetInvitation invitation) {
        AuthenticatedUser user = currentUserIdentity.current();

        if (user.email() == null || !user.email().equalsIgnoreCase(invitation.getEmail())) {
            throw new InvitationEmailMismatchException();
        }
        if (!user.emailVerified()) {
            throw new EmailNotVerifiedException();
        }
    }
}

package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByEmailUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPendingInvitationsByEmailService implements GetPendingInvitationsByEmailUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;
    private final CurrentUserIdentity currentUserIdentity;

    /**
     * Regla estricta de "lo mío" (PETS-02): un usuario solo ve las invitaciones pendientes para
     * su propio email. {@code request.email} llega {@code null} desde
     * {@code GET /pets/invitations/me}, o con el valor heredado (header o body) desde los alias
     * obsoletos, aceptado solo si coincide con el email del token (sin distinguir mayúsculas).
     */
    @Override
    public List<PetInvitationResponse> execute(GetPendingInvitationsByEmailRequest request) {
        String email = currentUserIdentity.resolveEmail(request.getEmail());

        return petInvitationRepositoryPort
                .findAllByEmailAndStatus(email, InvitationStatus.PENDING)
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

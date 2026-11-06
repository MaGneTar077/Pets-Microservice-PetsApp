package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedRequest;
import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsAlreadyInvitedUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IsAlreadyInvitedService implements IsAlreadyInvitedUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;
    private final PetAccessPolicy petAccessPolicy;

    /** {@code request.ownerId} es obsoleto (PETS-02): sondear invitaciones es exclusivo del dueño. */
    @Override
    public IsAlreadyInvitedResponse execute(IsAlreadyInvitedRequest request) {
        petAccessPolicy.requireOwner(request.getPetId(), request.getOwnerId());

        boolean invited = petInvitationRepositoryPort
                .findByPetIdAndEmailAndStatus(request.getPetId(), request.getEmail(), InvitationStatus.PENDING)
                .isPresent();

        return IsAlreadyInvitedResponse.builder()
                .invited(invited)
                .build();
    }
}

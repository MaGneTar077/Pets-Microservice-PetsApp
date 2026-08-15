package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedRequest;
import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsAlreadyInvitedUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IsAlreadyInvitedService implements IsAlreadyInvitedUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Override
    public IsAlreadyInvitedResponse execute(IsAlreadyInvitedRequest request) {
        boolean invited = petInvitationRepositoryPort
                .findByPetIdAndEmailAndStatus(request.getPetId(), request.getEmail(), InvitationStatus.PENDING)
                .isPresent();

        return IsAlreadyInvitedResponse.builder()
                .invited(invited)
                .build();
    }
}
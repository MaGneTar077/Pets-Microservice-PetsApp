package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsExpiredRequest;
import com.MyAnimaLog.Pets.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsExpiredUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IsExpiredService implements IsExpiredUseCase {

    private final PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Override
    public IsExpiredResponse execute(IsExpiredRequest request) {
        PetInvitation invitation = petInvitationRepositoryPort
                .findByToken(request.getToken())
                .orElseThrow(InvitationNotFoundException::new);

        boolean expired = LocalDateTime.now().isAfter(invitation.getExpiresAt());

        return IsExpiredResponse.builder()
                .expired(expired)
                .build();
    }
}

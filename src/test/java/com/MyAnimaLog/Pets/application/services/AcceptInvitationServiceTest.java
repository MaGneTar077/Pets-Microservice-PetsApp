package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.AccessAlreadyGrantedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptInvitationServiceTest {

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @InjectMocks
    private AcceptInvitationService acceptInvitationService;

    private UUID petId;
    private UUID userId;
    private String token;
    private PetInvitation invitation;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();
        token = UUID.randomUUID().toString();

        invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email("invitado@email.com")
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnResponse_whenRequestIsValid() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(PetUserAccess.builder()
                .id(UUID.randomUUID()).petId(petId).userId(userId)
                .accessRole(Rol.EDITOR).createdAt(LocalDateTime.now()).build());

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        AcceptInvitationResponse result = acceptInvitationService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAccessRole()).isEqualTo(Rol.EDITOR);
        assertThat(result.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void execute_shouldCallSaveOnInvitation_once() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(mock(PetUserAccess.class));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        acceptInvitationService.execute(request);

        verify(petInvitationRepositoryPort, times(1)).save(any(PetInvitation.class));
    }

    @Test
    void execute_shouldCallSaveOnPetUserAccess_once() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(mock(PetUserAccess.class));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        acceptInvitationService.execute(request);

        verify(petUserAccessRepositoryPort, times(1)).save(any(PetUserAccess.class));
    }

    @Test
    void execute_shouldThrowInvitationNotFoundException_whenTokenDoesNotExist() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.empty());

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        assertThatThrownBy(() -> acceptInvitationService.execute(request))
                .isInstanceOf(InvitationNotFoundException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowInvitationAlreadyProcessedException_whenStatusIsNotPending() {
        PetInvitation accepted = invitation.toBuilder().status(InvitationStatus.ACCEPTED).build();
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(accepted));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        assertThatThrownBy(() -> acceptInvitationService.execute(request))
                .isInstanceOf(InvitationAlreadyProcessedException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowInvitationAlreadyProcessedException_whenInvitationIsExpired() {
        PetInvitation expired = invitation.toBuilder()
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(expired));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        assertThatThrownBy(() -> acceptInvitationService.execute(request))
                .isInstanceOf(InvitationAlreadyProcessedException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowAccessAlreadyGrantedException_whenUserAlreadyHasAccess() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId))
                .thenReturn(Optional.of(mock(PetUserAccess.class)));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        assertThatThrownBy(() -> acceptInvitationService.execute(request))
                .isInstanceOf(AccessAlreadyGrantedException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }
}
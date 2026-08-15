package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.RejectInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
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
class RejectInvitationServiceTest {

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @InjectMocks
    private RejectInvitationService rejectInvitationService;

    private UUID petId;
    private String token;
    private PetInvitation invitation;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        token = UUID.randomUUID().toString();

        invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email("rechazado@email.com")
                .accessRole(Rol.VIEWER)
                .status(InvitationStatus.PENDING)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnResponse_whenRequestIsValid() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        RejectInvitationResponse result = rejectInvitationService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getInvitationId()).isEqualTo(invitation.getId());
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getStatus()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        rejectInvitationService.execute(request);

        verify(petInvitationRepositoryPort, times(1)).save(any(PetInvitation.class));
    }

    @Test
    void execute_shouldThrowInvitationNotFoundException_whenTokenDoesNotExist() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.empty());

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        assertThatThrownBy(() -> rejectInvitationService.execute(request))
                .isInstanceOf(InvitationNotFoundException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowInvitationAlreadyProcessedException_whenStatusIsNotPending() {
        PetInvitation accepted = invitation.toBuilder().status(InvitationStatus.ACCEPTED).build();
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(accepted));

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        assertThatThrownBy(() -> rejectInvitationService.execute(request))
                .isInstanceOf(InvitationAlreadyProcessedException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldNotCallSave_whenTokenNotFound() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.empty());

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        assertThatThrownBy(() -> rejectInvitationService.execute(request))
                .isInstanceOf(InvitationNotFoundException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }
}
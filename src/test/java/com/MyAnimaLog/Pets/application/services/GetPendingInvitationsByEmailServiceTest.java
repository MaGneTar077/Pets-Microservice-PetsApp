package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPendingInvitationsByEmailServiceTest {

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @InjectMocks
    private GetPendingInvitationsByEmailService getPendingInvitationsByEmailService;

    private String email;
    private PetInvitation invitation;

    @BeforeEach
    void setUp() {
        email = "invitado@email.com";

        invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(UUID.randomUUID())
                .email(email)
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnList_whenPendingInvitationsExist() {
        when(petInvitationRepositoryPort.findAllByEmailAndStatus(email, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));

        GetPendingInvitationsByEmailRequest request = GetPendingInvitationsByEmailRequest.builder()
                .email(email).build();

        List<PetInvitationResponse> result = getPendingInvitationsByEmailService.execute(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo(email);
        assertThat(result.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(result.get(0).getAccessRole()).isEqualTo(Rol.EDITOR);
    }

    @Test
    void execute_shouldReturnEmptyList_whenNoPendingInvitationsExist() {
        when(petInvitationRepositoryPort.findAllByEmailAndStatus(email, InvitationStatus.PENDING))
                .thenReturn(Collections.emptyList());

        GetPendingInvitationsByEmailRequest request = GetPendingInvitationsByEmailRequest.builder()
                .email(email).build();

        List<PetInvitationResponse> result = getPendingInvitationsByEmailService.execute(request);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_shouldCallRepositoryFindAllByEmailAndStatus_once() {
        when(petInvitationRepositoryPort.findAllByEmailAndStatus(email, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));

        GetPendingInvitationsByEmailRequest request = GetPendingInvitationsByEmailRequest.builder()
                .email(email).build();

        getPendingInvitationsByEmailService.execute(request);

        verify(petInvitationRepositoryPort, times(1))
                .findAllByEmailAndStatus(email, InvitationStatus.PENDING);
    }

    @Test
    void execute_shouldReturnMultipleInvitations_whenMultiplePendingExist() {
        PetInvitation invitation2 = invitation.toBuilder()
                .id(UUID.randomUUID())
                .petId(UUID.randomUUID())
                .accessRole(Rol.VIEWER)
                .build();

        when(petInvitationRepositoryPort.findAllByEmailAndStatus(email, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation, invitation2));

        GetPendingInvitationsByEmailRequest request = GetPendingInvitationsByEmailRequest.builder()
                .email(email).build();

        List<PetInvitationResponse> result = getPendingInvitationsByEmailService.execute(request);

        assertThat(result).hasSize(2);
    }
}
package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedRequest;
import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedResponse;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IsAlreadyInvitedServiceTest {

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @InjectMocks
    private IsAlreadyInvitedService isAlreadyInvitedService;

    private UUID petId;
    private String email;
    private PetInvitation invitation;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        email = "invitado@email.com";

        invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email(email)
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnTrue_whenPendingInvitationExists() {
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.of(invitation));

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).build();

        IsAlreadyInvitedResponse result = isAlreadyInvitedService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isInvited()).isTrue();
    }

    @Test
    void execute_shouldReturnFalse_whenNoPendingInvitationExists() {
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).build();

        IsAlreadyInvitedResponse result = isAlreadyInvitedService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isInvited()).isFalse();
    }

    @Test
    void execute_shouldCallRepositoryFindByPetIdAndEmailAndStatus_once() {
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).build();

        isAlreadyInvitedService.execute(request);

        verify(petInvitationRepositoryPort, times(1))
                .findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING);
    }

    @Test
    void execute_shouldReturnFalse_whenInvitationIsAccepted() {
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).build();

        IsAlreadyInvitedResponse result = isAlreadyInvitedService.execute(request);

        assertThat(result.isInvited()).isFalse();
    }
}
package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsExpiredRequest;
import com.MyAnimaLog.Pets.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IsExpiredServiceTest {

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @InjectMocks
    private IsExpiredService isExpiredService;

    private String token;
    private PetInvitation invitation;

    @BeforeEach
    void setUp() {
        token = UUID.randomUUID().toString();

        invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(UUID.randomUUID())
                .email("invitado@email.com")
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnFalse_whenInvitationIsNotExpired() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));

        IsExpiredRequest request = IsExpiredRequest.builder().token(token).build();

        IsExpiredResponse result = isExpiredService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isExpired()).isFalse();
    }

    @Test
    void execute_shouldReturnTrue_whenInvitationIsExpired() {
        PetInvitation expired = invitation.toBuilder()
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(expired));

        IsExpiredRequest request = IsExpiredRequest.builder().token(token).build();

        IsExpiredResponse result = isExpiredService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isExpired()).isTrue();
    }

    @Test
    void execute_shouldThrowInvitationNotFoundException_whenTokenDoesNotExist() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.empty());

        IsExpiredRequest request = IsExpiredRequest.builder().token(token).build();

        assertThatThrownBy(() -> isExpiredService.execute(request))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void execute_shouldCallRepositoryFindByToken_once() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));

        IsExpiredRequest request = IsExpiredRequest.builder().token(token).build();

        isExpiredService.execute(request);

        verify(petInvitationRepositoryPort, times(1)).findByToken(token);
    }
}
package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedRequest;
import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotBelongsToOwnerException;
import com.MyAnimaLog.Pets.domain.model.Pet;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IsAlreadyInvitedServiceTest {

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Mock
    private PetAccessPolicy petAccessPolicy;

    @InjectMocks
    private IsAlreadyInvitedService isAlreadyInvitedService;

    private UUID petId;
    private UUID ownerId;
    private String email;
    private PetInvitation invitation;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        email = "invitado@email.com";

        lenient().when(petAccessPolicy.requireOwner(eq(petId), any()))
                .thenReturn(Pet.builder().id(petId).ownerId(ownerId).name("Firulais").species("Perro").build());

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
                .petId(petId).email(email).ownerId(ownerId).build();

        IsAlreadyInvitedResponse result = isAlreadyInvitedService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isInvited()).isTrue();
    }

    @Test
    void execute_shouldReturnFalse_whenNoPendingInvitationExists() {
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).ownerId(ownerId).build();

        IsAlreadyInvitedResponse result = isAlreadyInvitedService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isInvited()).isFalse();
    }

    @Test
    void execute_shouldCallRepositoryFindByPetIdAndEmailAndStatus_once() {
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).ownerId(ownerId).build();

        isAlreadyInvitedService.execute(request);

        verify(petInvitationRepositoryPort, times(1))
                .findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING);
    }

    @Test
    void execute_shouldReturnFalse_whenInvitationIsAccepted() {
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).ownerId(ownerId).build();

        IsAlreadyInvitedResponse result = isAlreadyInvitedService.execute(request);

        assertThat(result.isInvited()).isFalse();
    }

    @Test
    void execute_shouldThrow403_whenRequesterIsNotOwner() {
        when(petAccessPolicy.requireOwner(petId, ownerId)).thenThrow(new PetNotBelongsToOwnerException());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).ownerId(ownerId).build();

        assertThatThrownBy(() -> isAlreadyInvitedService.execute(request))
                .isInstanceOf(PetNotBelongsToOwnerException.class);

        verify(petInvitationRepositoryPort, never()).findByPetIdAndEmailAndStatus(any(), any(), any());
    }

    @Test
    void execute_shouldThrow403_whenOwnerIdIsUnrelated() {
        UUID unrelatedOwner = UUID.randomUUID();
        when(petAccessPolicy.requireOwner(petId, unrelatedOwner)).thenThrow(new IdentityMismatchException());

        IsAlreadyInvitedRequest request = IsAlreadyInvitedRequest.builder()
                .petId(petId).email(email).ownerId(unrelatedOwner).build();

        assertThatThrownBy(() -> isAlreadyInvitedService.execute(request))
                .isInstanceOf(IdentityMismatchException.class);

        verify(petInvitationRepositoryPort, never()).findByPetIdAndEmailAndStatus(any(), any(), any());
    }
}

package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Pets.application.dto.AuthenticatedUser;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.AccessAlreadyGrantedException;
import com.MyAnimaLog.Pets.domain.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
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

    private static final String INVITATION_EMAIL = "invitado@email.com";

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PublishPetEventUseCase publishPetEventUseCase;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @InjectMocks
    private AcceptInvitationService acceptInvitationService;

    private UUID petId;
    private UUID userId;
    private String token;
    private PetInvitation invitation;
    private Pet pet;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();
        token = UUID.randomUUID().toString();

        lenient().when(currentUserIdentity.resolveUserId(any())).thenReturn(userId);
        lenient().when(currentUserIdentity.current())
                .thenReturn(new AuthenticatedUser(userId, INVITATION_EMAIL, true));

        invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email(INVITATION_EMAIL)
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        pet = Pet.builder()
                .id(petId)
                .ownerId(UUID.randomUUID())
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
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
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

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
    void execute_shouldWork_whenTheRequestDoesNotSendUserId() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(mock(PetUserAccess.class));
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        AcceptInvitationResponse result = acceptInvitationService.execute(
                AcceptInvitationRequest.builder().token(token).build());

        assertThat(result.getUserId()).isEqualTo(userId);
        verify(currentUserIdentity).resolveUserId(null);
    }

    @Test
    void execute_shouldThrow403_whenTheRequestSendsAnUnrelatedUserId() {
        UUID unrelatedUser = UUID.randomUUID();
        when(currentUserIdentity.resolveUserId(unrelatedUser)).thenThrow(new IdentityMismatchException());

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(unrelatedUser).build();

        assertThatThrownBy(() -> acceptInvitationService.execute(request))
                .isInstanceOf(IdentityMismatchException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldCallSaveOnInvitation_once() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(mock(PetUserAccess.class));
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

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
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

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
    void execute_shouldThrowInvitationEmailMismatchException_whenTokenEmailDiffersFromInvitation() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(currentUserIdentity.current())
                .thenReturn(new AuthenticatedUser(userId, "otro@correo.com", true));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        assertThatThrownBy(() -> acceptInvitationService.execute(request))
                .isInstanceOf(InvitationEmailMismatchException.class);

        verify(petUserAccessRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldAccept_whenTokenEmailDiffersOnlyInCase() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(currentUserIdentity.current())
                .thenReturn(new AuthenticatedUser(userId, INVITATION_EMAIL.toUpperCase(), true));
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petUserAccessRepositoryPort.save(any(PetUserAccess.class))).thenReturn(mock(PetUserAccess.class));
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        assertThatNoException().isThrownBy(() -> acceptInvitationService.execute(request));
    }

    @Test
    void execute_shouldThrowEmailNotVerifiedException_whenTokenEmailIsNotVerified() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(currentUserIdentity.current())
                .thenReturn(new AuthenticatedUser(userId, INVITATION_EMAIL, false));

        AcceptInvitationRequest request = AcceptInvitationRequest.builder()
                .token(token).userId(userId).build();

        assertThatThrownBy(() -> acceptInvitationService.execute(request))
                .isInstanceOf(EmailNotVerifiedException.class);

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

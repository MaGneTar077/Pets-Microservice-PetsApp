package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.RejectInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RejectInvitationServiceTest {

    private static final String INVITATION_EMAIL = "rechazado@email.com";

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PublishPetEventUseCase publishPetEventUseCase;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @InjectMocks
    private RejectInvitationService rejectInvitationService;

    private UUID petId;
    private String token;
    private PetInvitation invitation;
    private Pet pet;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        token = UUID.randomUUID().toString();

        lenient().when(currentUserIdentity.email()).thenReturn(INVITATION_EMAIL);
        lenient().when(currentUserIdentity.emailVerified()).thenReturn(true);

        invitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email(INVITATION_EMAIL)
                .accessRole(Rol.VIEWER)
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
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

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
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

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
    void execute_shouldThrowInvitationEmailMismatchException_whenTokenEmailDiffersFromInvitation() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(currentUserIdentity.email()).thenReturn("otro@correo.com");

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        assertThatThrownBy(() -> rejectInvitationService.execute(request))
                .isInstanceOf(InvitationEmailMismatchException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldReject_whenTokenEmailDiffersOnlyInCase() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(currentUserIdentity.email()).thenReturn(INVITATION_EMAIL.toUpperCase());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(invitation);
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        assertThatNoException().isThrownBy(() -> rejectInvitationService.execute(request));
    }

    @Test
    void execute_shouldThrowEmailNotVerifiedException_whenTokenEmailIsNotVerified() {
        when(petInvitationRepositoryPort.findByToken(token)).thenReturn(Optional.of(invitation));
        when(currentUserIdentity.emailVerified()).thenReturn(false);

        RejectInvitationRequest request = RejectInvitationRequest.builder()
                .token(token).build();

        assertThatThrownBy(() -> rejectInvitationService.execute(request))
                .isInstanceOf(EmailNotVerifiedException.class);

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

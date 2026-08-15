package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.SendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.SendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyExistsException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendInvitationServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @InjectMocks
    private SendInvitationService sendInvitationService;

    private UUID petId;
    private UUID ownerId;
    private Pet pet;
    private PetInvitation savedInvitation;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        pet = Pet.builder()
                .id(petId)
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        savedInvitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email("invitado@email.com")
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnResponse_whenRequestIsValid() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, "invitado@email.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(savedInvitation);

        SendInvitationRequest request = SendInvitationRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .email("invitado@email.com")
                .accessRole(Rol.EDITOR)
                .build();

        SendInvitationResponse result = sendInvitationService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getEmail()).isEqualTo("invitado@email.com");
        assertThat(result.getAccessRole()).isEqualTo(Rol.EDITOR);
        assertThat(result.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getExpiresAt()).isNotNull();
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, "invitado@email.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(petInvitationRepositoryPort.save(any(PetInvitation.class))).thenReturn(savedInvitation);

        SendInvitationRequest request = SendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email("invitado@email.com").accessRole(Rol.EDITOR).build();

        sendInvitationService.execute(request);

        verify(petInvitationRepositoryPort, times(1)).save(any(PetInvitation.class));
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        SendInvitationRequest request = SendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email("invitado@email.com").accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> sendInvitationService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowUnauthorizedPetAccessException_whenRequesterIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        SendInvitationRequest request = SendInvitationRequest.builder()
                .petId(petId).ownerId(otherUser).email("invitado@email.com").accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> sendInvitationService.execute(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowInvitationAlreadyExistsException_whenPendingInvitationExists() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findByPetIdAndEmailAndStatus(petId, "invitado@email.com", InvitationStatus.PENDING))
                .thenReturn(Optional.of(savedInvitation));

        SendInvitationRequest request = SendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email("invitado@email.com").accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> sendInvitationService.execute(request))
                .isInstanceOf(InvitationAlreadyExistsException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldNotCallSave_whenPetNotFound() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        SendInvitationRequest request = SendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email("invitado@email.com").accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> sendInvitationService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }
}
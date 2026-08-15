package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.ResendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.ResendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendInvitationServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @InjectMocks
    private ResendInvitationService resendInvitationService;

    private UUID petId;
    private UUID ownerId;
    private String email;
    private Pet pet;
    private PetInvitation existingInvitation;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        email = "rechazado@email.com";

        pet = Pet.builder()
                .id(petId)
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        existingInvitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email(email)
                .accessRole(Rol.VIEWER)
                .status(InvitationStatus.REJECTED)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();
    }

    @Test
    void execute_shouldReturnResponse_whenRequestIsValid() {
        PetInvitation newInvitation = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email(email)
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findAllByPetIdAndEmail(petId, email))
                .thenReturn(List.of(existingInvitation));
        when(petInvitationRepositoryPort.save(any(PetInvitation.class)))
                .thenReturn(existingInvitation).thenReturn(newInvitation);

        ResendInvitationRequest request = ResendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email(email).accessRole(Rol.EDITOR).build();

        ResendInvitationResponse result = resendInvitationService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void execute_shouldCancelExistingInvitations_beforeCreatingNew() {
        PetInvitation newInvitation = existingInvitation.toBuilder()
                .id(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findAllByPetIdAndEmail(petId, email))
                .thenReturn(List.of(existingInvitation));
        when(petInvitationRepositoryPort.save(any(PetInvitation.class)))
                .thenReturn(existingInvitation).thenReturn(newInvitation);

        ResendInvitationRequest request = ResendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email(email).accessRole(Rol.EDITOR).build();

        resendInvitationService.execute(request);

        verify(petInvitationRepositoryPort, times(2)).save(any(PetInvitation.class));
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        ResendInvitationRequest request = ResendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email(email).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> resendInvitationService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowUnauthorizedPetAccessException_whenRequesterIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        ResendInvitationRequest request = ResendInvitationRequest.builder()
                .petId(petId).ownerId(otherUser).email(email).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> resendInvitationService.execute(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }

    @Test
    void execute_shouldThrowInvitationNotFoundException_whenNoInvitationExists() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findAllByPetIdAndEmail(petId, email))
                .thenReturn(Collections.emptyList());

        ResendInvitationRequest request = ResendInvitationRequest.builder()
                .petId(petId).ownerId(ownerId).email(email).accessRole(Rol.EDITOR).build();

        assertThatThrownBy(() -> resendInvitationService.execute(request))
                .isInstanceOf(InvitationNotFoundException.class);

        verify(petInvitationRepositoryPort, never()).save(any());
    }
}
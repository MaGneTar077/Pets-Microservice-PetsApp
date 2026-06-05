package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetInvitationRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPendingInvitationsByPetServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetInvitationRepositoryPort petInvitationRepositoryPort;

    @InjectMocks
    private GetPendingInvitationsByPetService getPendingInvitationsByPetService;

    private UUID petId;
    private UUID ownerId;
    private Pet pet;
    private PetInvitation invitation;

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

        invitation = PetInvitation.builder()
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
    void execute_shouldReturnList_whenRequestIsValid() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findAllByPetIdAndStatus(petId, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));

        GetPendingInvitationsByPetRequest request = GetPendingInvitationsByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        List<PetInvitationResponse> result = getPendingInvitationsByPetService.execute(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPetId()).isEqualTo(petId);
        assertThat(result.get(0).getEmail()).isEqualTo("invitado@email.com");
        assertThat(result.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(result.get(0).getAccessRole()).isEqualTo(Rol.EDITOR);
    }

    @Test
    void execute_shouldReturnEmptyList_whenNoPendingInvitations() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findAllByPetIdAndStatus(petId, InvitationStatus.PENDING))
                .thenReturn(Collections.emptyList());

        GetPendingInvitationsByPetRequest request = GetPendingInvitationsByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        List<PetInvitationResponse> result = getPendingInvitationsByPetService.execute(request);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_shouldCallRepositoryFindAllByPetIdAndStatus_once() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
        when(petInvitationRepositoryPort.findAllByPetIdAndStatus(petId, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));

        GetPendingInvitationsByPetRequest request = GetPendingInvitationsByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        getPendingInvitationsByPetService.execute(request);

        verify(petInvitationRepositoryPort, times(1))
                .findAllByPetIdAndStatus(petId, InvitationStatus.PENDING);
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        GetPendingInvitationsByPetRequest request = GetPendingInvitationsByPetRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        assertThatThrownBy(() -> getPendingInvitationsByPetService.execute(request))
                .isInstanceOf(PetNotFoundException.class);

        verify(petInvitationRepositoryPort, never()).findAllByPetIdAndStatus(any(), any());
    }

    @Test
    void execute_shouldThrowUnauthorizedPetAccessException_whenRequesterIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));

        GetPendingInvitationsByPetRequest request = GetPendingInvitationsByPetRequest.builder()
                .petId(petId).ownerId(otherUser).build();

        assertThatThrownBy(() -> getPendingInvitationsByPetService.execute(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petInvitationRepositoryPort, never()).findAllByPetIdAndStatus(any(), any());
    }
}
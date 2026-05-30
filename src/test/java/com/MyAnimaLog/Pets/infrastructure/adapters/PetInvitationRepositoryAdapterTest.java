package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.model.PetInvitation;
import com.MyAnimaLog.Pets.infrastructure.entity.PetInvitationEntity;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetInvitationMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetInvitationRepositoryAdapterTest {

    @Mock
    private JpaPetInvitationRepository jpaPetInvitationRepository;

    @Mock
    private PetInvitationMapper petInvitationMapper;

    @InjectMocks
    private PetInvitationRepositoryAdapter petInvitationRepositoryAdapter;

    private UUID petId;
    private String email;
    private PetInvitation domain;
    private PetInvitationEntity entity;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        email = "invitado@email.com";

        domain = PetInvitation.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email(email)
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        entity = PetInvitationEntity.builder()
                .id(domain.getId())
                .petId(petId)
                .email(email)
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(domain.getToken())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    @Test
    void save_shouldReturnDomain_whenEntityIsSaved() {
        when(petInvitationMapper.toEntity(any(PetInvitation.class))).thenReturn(entity);
        when(jpaPetInvitationRepository.save(any(PetInvitationEntity.class))).thenReturn(entity);
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        PetInvitation result = petInvitationRepositoryAdapter.save(domain);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void save_shouldCallMapperToEntity_once() {
        when(petInvitationMapper.toEntity(any(PetInvitation.class))).thenReturn(entity);
        when(jpaPetInvitationRepository.save(any(PetInvitationEntity.class))).thenReturn(entity);
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        petInvitationRepositoryAdapter.save(domain);

        verify(petInvitationMapper, times(1)).toEntity(any(PetInvitation.class));
    }

    @Test
    void save_shouldCallJpaRepository_once() {
        when(petInvitationMapper.toEntity(any(PetInvitation.class))).thenReturn(entity);
        when(jpaPetInvitationRepository.save(any(PetInvitationEntity.class))).thenReturn(entity);
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        petInvitationRepositoryAdapter.save(domain);

        verify(jpaPetInvitationRepository, times(1)).save(any(PetInvitationEntity.class));
    }

    @Test
    void save_shouldCallMapperToDomain_once() {
        when(petInvitationMapper.toEntity(any(PetInvitation.class))).thenReturn(entity);
        when(jpaPetInvitationRepository.save(any(PetInvitationEntity.class))).thenReturn(entity);
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        petInvitationRepositoryAdapter.save(domain);

        verify(petInvitationMapper, times(1)).toDomain(any(PetInvitationEntity.class));
    }

    @Test
    void findById_shouldReturnDomain_whenFound() {
        when(jpaPetInvitationRepository.findById(domain.getId())).thenReturn(Optional.of(entity));
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        Optional<PetInvitation> result = petInvitationRepositoryAdapter.findById(domain.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        when(jpaPetInvitationRepository.findById(domain.getId())).thenReturn(Optional.empty());

        Optional<PetInvitation> result = petInvitationRepositoryAdapter.findById(domain.getId());

        assertThat(result).isEmpty();
        verify(petInvitationMapper, never()).toDomain(any());
    }

    @Test
    void findByPetIdAndEmailAndStatus_shouldReturnDomain_whenFound() {
        when(jpaPetInvitationRepository.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.of(entity));
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        Optional<PetInvitation> result = petInvitationRepositoryAdapter
                .findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void findByPetIdAndEmailAndStatus_shouldReturnEmpty_whenNotFound() {
        when(jpaPetInvitationRepository.findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        Optional<PetInvitation> result = petInvitationRepositoryAdapter
                .findByPetIdAndEmailAndStatus(petId, email, InvitationStatus.PENDING);

        assertThat(result).isEmpty();
        verify(petInvitationMapper, never()).toDomain(any());
    }

    @Test
    void findAllByPetIdAndStatus_shouldReturnList_whenFound() {
        when(jpaPetInvitationRepository.findAllByPetIdAndStatus(petId, InvitationStatus.PENDING))
                .thenReturn(List.of(entity));
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        List<PetInvitation> result = petInvitationRepositoryAdapter
                .findAllByPetIdAndStatus(petId, InvitationStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPetId()).isEqualTo(petId);
    }

    @Test
    void findAllByEmailAndStatus_shouldReturnList_whenFound() {
        when(jpaPetInvitationRepository.findAllByEmailAndStatus(email, InvitationStatus.PENDING))
                .thenReturn(List.of(entity));
        when(petInvitationMapper.toDomain(any(PetInvitationEntity.class))).thenReturn(domain);

        List<PetInvitation> result = petInvitationRepositoryAdapter
                .findAllByEmailAndStatus(email, InvitationStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo(email);
    }
}
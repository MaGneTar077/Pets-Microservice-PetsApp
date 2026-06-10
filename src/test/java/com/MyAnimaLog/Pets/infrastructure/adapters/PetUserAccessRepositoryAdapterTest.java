package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import com.MyAnimaLog.Pets.infrastructure.entity.PetUserAccessEntity;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetUserAccessMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetUserAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetUserAccessRepositoryAdapterTest {

    @Mock
    private JpaPetUserAccessRepository jpaPetUserAccessRepository;

    @Mock
    private PetUserAccessMapper petUserAccessMapper;

    @InjectMocks
    private PetUserAccessRepositoryAdapter petUserAccessRepositoryAdapter;

    private UUID petId;
    private UUID userId;
    private PetUserAccess domain;
    private PetUserAccessEntity entity;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();

        domain = PetUserAccess.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .userId(userId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();

        entity = PetUserAccessEntity.builder()
                .id(domain.getId())
                .petId(petId)
                .userId(userId)
                .accessRole(Rol.EDITOR)
                .createdAt(domain.getCreatedAt())
                .build();
    }

    @Test
    void save_shouldReturnDomain_whenEntityIsSaved() {
        when(petUserAccessMapper.toEntity(any(PetUserAccess.class))).thenReturn(entity);
        when(jpaPetUserAccessRepository.save(any(PetUserAccessEntity.class))).thenReturn(entity);
        when(petUserAccessMapper.toDomain(any(PetUserAccessEntity.class))).thenReturn(domain);

        PetUserAccess result = petUserAccessRepositoryAdapter.save(domain);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAccessRole()).isEqualTo(Rol.EDITOR);
    }

    @Test
    void save_shouldCallMapperToEntity_once() {
        when(petUserAccessMapper.toEntity(any(PetUserAccess.class))).thenReturn(entity);
        when(jpaPetUserAccessRepository.save(any(PetUserAccessEntity.class))).thenReturn(entity);
        when(petUserAccessMapper.toDomain(any(PetUserAccessEntity.class))).thenReturn(domain);

        petUserAccessRepositoryAdapter.save(domain);

        verify(petUserAccessMapper, times(1)).toEntity(any(PetUserAccess.class));
    }

    @Test
    void save_shouldCallJpaRepository_once() {
        when(petUserAccessMapper.toEntity(any(PetUserAccess.class))).thenReturn(entity);
        when(jpaPetUserAccessRepository.save(any(PetUserAccessEntity.class))).thenReturn(entity);
        when(petUserAccessMapper.toDomain(any(PetUserAccessEntity.class))).thenReturn(domain);

        petUserAccessRepositoryAdapter.save(domain);

        verify(jpaPetUserAccessRepository, times(1)).save(any(PetUserAccessEntity.class));
    }

    @Test
    void save_shouldCallMapperToDomain_once() {
        when(petUserAccessMapper.toEntity(any(PetUserAccess.class))).thenReturn(entity);
        when(jpaPetUserAccessRepository.save(any(PetUserAccessEntity.class))).thenReturn(entity);
        when(petUserAccessMapper.toDomain(any(PetUserAccessEntity.class))).thenReturn(domain);

        petUserAccessRepositoryAdapter.save(domain);

        verify(petUserAccessMapper, times(1)).toDomain(any(PetUserAccessEntity.class));
    }

    @Test
    void findByPetIdAndUserId_shouldReturnDomain_whenFound() {
        when(jpaPetUserAccessRepository.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.of(entity));
        when(petUserAccessMapper.toDomain(any(PetUserAccessEntity.class))).thenReturn(domain);

        Optional<PetUserAccess> result = petUserAccessRepositoryAdapter.findByPetIdAndUserId(petId, userId);

        assertThat(result).isPresent();
        assertThat(result.get().getPetId()).isEqualTo(petId);
        assertThat(result.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findByPetIdAndUserId_shouldReturnEmpty_whenNotFound() {
        when(jpaPetUserAccessRepository.findByPetIdAndUserId(petId, userId)).thenReturn(Optional.empty());

        Optional<PetUserAccess> result = petUserAccessRepositoryAdapter.findByPetIdAndUserId(petId, userId);

        assertThat(result).isEmpty();
        verify(petUserAccessMapper, never()).toDomain(any());
    }

    @Test
    void deleteByPetIdAndUserId_shouldCallJpaRepository_once() {
        doNothing().when(jpaPetUserAccessRepository).deleteByPetIdAndUserId(petId, userId);

        petUserAccessRepositoryAdapter.deleteByPetIdAndUserId(petId, userId);

        verify(jpaPetUserAccessRepository, times(1)).deleteByPetIdAndUserId(petId, userId);
    }

    @Test
    void findAllByUserId_shouldReturnList_whenFound() {
        when(jpaPetUserAccessRepository.findAllByUserId(userId)).thenReturn(List.of(entity));
        when(petUserAccessMapper.toDomain(any(PetUserAccessEntity.class))).thenReturn(domain);

        List<PetUserAccess> result = petUserAccessRepositoryAdapter.findAllByUserId(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void findAllByUserId_shouldReturnEmptyList_whenNotFound() {
        when(jpaPetUserAccessRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        List<PetUserAccess> result = petUserAccessRepositoryAdapter.findAllByUserId(userId);

        assertThat(result).isEmpty();
        verify(petUserAccessMapper, never()).toDomain(any());
    }
}
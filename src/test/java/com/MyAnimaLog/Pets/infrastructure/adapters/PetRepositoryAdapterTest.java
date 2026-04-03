package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.entity.PetEntity;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import com.MyAnimaLog.Pets.infrastructure.repositories.JpaPetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetRepositoryAdapterTest {

    @Mock
    private JpaPetRepository jpaPetRepository;

    @Mock
    private PetMapper petMapper;

    @InjectMocks
    private PetRepositoryAdapter petRepositoryAdapter;

    private Pet pet;
    private PetEntity petEntity;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        pet = Pet.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .breed("Labrador")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .height(45.5)
                .weight(12.3)
                .createdAt(LocalDateTime.now())
                .build();

        petEntity = PetEntity.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .breed("Labrador")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .height(45.5)
                .weight(12.3)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void save_shouldReturnPet_whenEntityIsSaved() {
        when(petMapper.toEntity(any(Pet.class))).thenReturn(petEntity);
        when(jpaPetRepository.save(any(PetEntity.class))).thenReturn(petEntity);
        when(petMapper.toDomain(any(PetEntity.class))).thenReturn(pet);

        Pet result = petRepositoryAdapter.save(pet);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Firulais");
        assertThat(result.getSpecies()).isEqualTo("Perro");
    }

    @Test
    void save_shouldCallMapperToEntity_once() {
        when(petMapper.toEntity(any(Pet.class))).thenReturn(petEntity);
        when(jpaPetRepository.save(any(PetEntity.class))).thenReturn(petEntity);
        when(petMapper.toDomain(any(PetEntity.class))).thenReturn(pet);

        petRepositoryAdapter.save(pet);

        verify(petMapper, times(1)).toEntity(any(Pet.class));
    }

    @Test
    void save_shouldCallJpaRepository_once() {
        when(petMapper.toEntity(any(Pet.class))).thenReturn(petEntity);
        when(jpaPetRepository.save(any(PetEntity.class))).thenReturn(petEntity);
        when(petMapper.toDomain(any(PetEntity.class))).thenReturn(pet);

        petRepositoryAdapter.save(pet);

        verify(jpaPetRepository, times(1)).save(any(PetEntity.class));
    }

    @Test
    void save_shouldCallMapperToDomain_once() {
        when(petMapper.toEntity(any(Pet.class))).thenReturn(petEntity);
        when(jpaPetRepository.save(any(PetEntity.class))).thenReturn(petEntity);
        when(petMapper.toDomain(any(PetEntity.class))).thenReturn(pet);

        petRepositoryAdapter.save(pet);

        verify(petMapper, times(1)).toDomain(any(PetEntity.class));
    }
}
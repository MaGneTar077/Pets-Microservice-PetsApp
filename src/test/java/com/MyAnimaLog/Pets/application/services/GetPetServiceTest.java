package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
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
class GetPetServiceTest {

    @Mock
    private PetRepositoryPort petRepository;

    @Mock
    private PetMapper petMapper;

    @InjectMocks
    private GetPetService getPetService;

    private Pet pet;
    private PetResponse petResponse;
    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();

        pet = Pet.builder()
                .id(petId)
                .ownerId(UUID.randomUUID())
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .createdAt(LocalDateTime.now())
                .build();

        petResponse = PetResponse.builder()
                .id(petId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .build();
    }

    @Test
    void getPetById_shouldReturnPetResponse_whenPetExists() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petMapper.toResponse(pet)).thenReturn(petResponse);

        PetResponse result = getPetService.getPetById(petId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(petId);
        assertThat(result.getName()).isEqualTo("Firulais");
    }

    @Test
    void getPetById_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getPetService.getPetById(petId))
                .isInstanceOf(PetNotFoundException.class);
    }

    @Test
    void getPetById_shouldCallFindById_once() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petMapper.toResponse(pet)).thenReturn(petResponse);

        getPetService.getPetById(petId);

        verify(petRepository, times(1)).findById(petId);
    }

    @Test
    void getPetById_shouldCallMapperToResponse_once() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petMapper.toResponse(pet)).thenReturn(petResponse);

        getPetService.getPetById(petId);

        verify(petMapper, times(1)).toResponse(pet);
    }
}
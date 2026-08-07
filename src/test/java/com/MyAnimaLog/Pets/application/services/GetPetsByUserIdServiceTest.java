package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPetsByUserIdServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetMapper petMapper;

    @InjectMocks
    private GetPetsByUserIdService getPetsByUserIdService;

    private UUID ownerId;
    private Pet pet1;
    private Pet pet2;
    private PetResponse response1;
    private PetResponse response2;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();

        pet1 = Pet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .breed("Labrador")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .height(45.5)
                .weight(12.3)
                .build();

        pet2 = Pet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Michi")
                .species("Gato")
                .breed("Siames")
                .sex(Sex.FEMALE)
                .birthDate(LocalDate.of(2021, 5, 10))
                .height(20.0)
                .weight(4.5)
                .build();

        response1 = PetResponse.builder()
                .id(pet1.getId())
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .build();

        response2 = PetResponse.builder()
                .id(pet2.getId())
                .ownerId(ownerId)
                .name("Michi")
                .species("Gato")
                .sex(Sex.FEMALE)
                .build();
    }

    @Test
    void getPetsByUserId_shouldReturnListOfPetResponses_whenUserHasPets() {
        when(petRepositoryPort.findAllByOwnerId(ownerId)).thenReturn(List.of(pet1, pet2));
        when(petMapper.toResponse(pet1)).thenReturn(response1);
        when(petMapper.toResponse(pet2)).thenReturn(response2);

        List<PetResponse> result = getPetsByUserIdService.getPetsByUserId(ownerId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PetResponse::getName)
                .containsExactly("Firulais", "Michi");
    }

    @Test
    void getPetsByUserId_shouldReturnEmptyList_whenUserHasNoPets() {
        when(petRepositoryPort.findAllByOwnerId(ownerId)).thenReturn(Collections.emptyList());

        List<PetResponse> result = getPetsByUserIdService.getPetsByUserId(ownerId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(petMapper, never()).toResponse(any(Pet.class));
    }

    @Test
    void getPetsByUserId_shouldCallRepositoryFindAllByOwnerId_once() {
        when(petRepositoryPort.findAllByOwnerId(ownerId)).thenReturn(List.of(pet1));
        when(petMapper.toResponse(pet1)).thenReturn(response1);

        getPetsByUserIdService.getPetsByUserId(ownerId);

        verify(petRepositoryPort, times(1)).findAllByOwnerId(ownerId);
    }

    @Test
    void getPetsByUserId_shouldReturnPetsOnlyBelongingToOwnerId() {
        when(petRepositoryPort.findAllByOwnerId(ownerId)).thenReturn(List.of(pet1, pet2));
        when(petMapper.toResponse(pet1)).thenReturn(response1);
        when(petMapper.toResponse(pet2)).thenReturn(response2);

        List<PetResponse> result = getPetsByUserIdService.getPetsByUserId(ownerId);

        assertThat(result).allMatch(petResponse -> petResponse.getOwnerId().equals(ownerId));
    }
}
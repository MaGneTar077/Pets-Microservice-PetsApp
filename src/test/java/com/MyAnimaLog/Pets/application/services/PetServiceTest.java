package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.AddPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.model.Pet;
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
class PetServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @InjectMocks
    private PetService petService;

    private AddPetRequest request;
    private Pet savedPet;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();

        request = new AddPetRequest();
        request.setOwnerId(ownerId);
        request.setName("Firulais");
        request.setSpecies("Perro");
        request.setBreed("Labrador");
        request.setSex(Sex.MALE);
        request.setBirthDate(LocalDate.of(2022, 1, 15));
        request.setHeight(45.5);
        request.setWeight(12.3);

        savedPet = Pet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .breed("Labrador")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .height(45.5)
                .weight(12.3)
                .photoUrl(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addPet_shouldReturnPetResponse_whenRequestIsValid() {
        when(petRepositoryPort.save(any(Pet.class))).thenReturn(savedPet);

        PetResponse response = petService.add(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Firulais");
        assertThat(response.getSpecies()).isEqualTo("Perro");
        assertThat(response.getBreed()).isEqualTo("Labrador");
        assertThat(response.getSex()).isEqualTo(Sex.MALE);
        assertThat(response.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void addPet_shouldCallRepositorySave_once() {
        when(petRepositoryPort.save(any(Pet.class))).thenReturn(savedPet);

        petService.add(request);

        verify(petRepositoryPort, times(1)).save(any(Pet.class));
    }

    @Test
    void addPet_shouldSetCreatedAt_whenSaving() {
        when(petRepositoryPort.save(any(Pet.class))).thenReturn(savedPet);

        PetResponse response = petService.add(request);

        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void addPet_shouldReturnPetResponse_withCorrectId() {
        when(petRepositoryPort.save(any(Pet.class))).thenReturn(savedPet);

        PetResponse response = petService.add(request);

        assertThat(response.getId()).isEqualTo(savedPet.getId());
    }

    @Test
    void addPet_shouldReturnPetResponse_withNullPhotoUrl_whenNotProvided() {
        when(petRepositoryPort.save(any(Pet.class))).thenReturn(savedPet);

        PetResponse response = petService.add(request);

        assertThat(response.getPhotoUrl()).isNull();
    }
}
package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.AgeResponse;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.BirthDateNotRegisteredException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculateAgeServiceTest {

    @Mock
    private PetAccessPolicy petAccessPolicy;

    @InjectMocks
    private CalculateAgeService calculateAgeService;

    private Pet petWithBirthDate;
    private Pet petWithoutBirthDate;
    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();

        petWithBirthDate = Pet.builder()
                .id(petId)
                .ownerId(UUID.randomUUID())
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        petWithoutBirthDate = Pet.builder()
                .id(petId)
                .ownerId(UUID.randomUUID())
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void calculateAge_shouldReturnAgeResponse_whenCallerIsTheOwner() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(petWithBirthDate);

        AgeResponse result = calculateAgeService.calculateAge(petId);

        assertThat(result).isNotNull();
        assertThat(result.getYears()).isGreaterThanOrEqualTo(0);
        assertThat(result.getMonths()).isBetween(0, 11);
        assertThat(result.getDays()).isBetween(0, 30);
    }

    @Test
    void calculateAge_shouldReturnAgeResponse_whenCallerIsAnEditor() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(petWithBirthDate);

        assertThat(calculateAgeService.calculateAge(petId)).isNotNull();
    }

    @Test
    void calculateAge_shouldReturnAgeResponse_whenCallerIsAViewer() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(petWithBirthDate);

        assertThat(calculateAgeService.calculateAge(petId)).isNotNull();
    }

    @Test
    void calculateAge_shouldThrow403_whenCallerHasNoAccess() {
        when(petAccessPolicy.requireRead(petId)).thenThrow(new UnauthorizedPetAccessException());

        assertThatThrownBy(() -> calculateAgeService.calculateAge(petId))
                .isInstanceOf(UnauthorizedPetAccessException.class);
    }

    @Test
    void calculateAge_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petAccessPolicy.requireRead(petId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() -> calculateAgeService.calculateAge(petId))
                .isInstanceOf(PetNotFoundException.class);
    }

    @Test
    void calculateAge_shouldThrowBirthDateNotRegisteredException_whenBirthDateIsNull() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(petWithoutBirthDate);

        assertThatThrownBy(() -> calculateAgeService.calculateAge(petId))
                .isInstanceOf(BirthDateNotRegisteredException.class);
    }
}

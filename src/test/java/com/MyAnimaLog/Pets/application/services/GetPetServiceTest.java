package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.infrastructure.mapper.PetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPetServiceTest {

    @Mock
    private PetMapper petMapper;

    @Mock
    private PetAccessPolicy petAccessPolicy;

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
    void getPetById_shouldReturnPetResponse_whenCallerIsTheOwner() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petMapper.toResponse(pet)).thenReturn(petResponse);

        PetResponse result = getPetService.getPetById(petId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(petId);
        assertThat(result.getName()).isEqualTo("Firulais");
    }

    @Test
    void getPetById_shouldReturnPetResponse_whenCallerIsAnEditor() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petMapper.toResponse(pet)).thenReturn(petResponse);

        assertThat(getPetService.getPetById(petId)).isEqualTo(petResponse);
    }

    @Test
    void getPetById_shouldReturnPetResponse_whenCallerIsAViewer() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petMapper.toResponse(pet)).thenReturn(petResponse);

        assertThat(getPetService.getPetById(petId)).isEqualTo(petResponse);
    }

    @Test
    void getPetById_shouldThrow403_whenCallerHasNoAccess() {
        when(petAccessPolicy.requireRead(petId)).thenThrow(new UnauthorizedPetAccessException());

        assertThatThrownBy(() -> getPetService.getPetById(petId))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verifyNoInteractions(petMapper);
    }

    @Test
    void getPetById_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petAccessPolicy.requireRead(petId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() -> getPetService.getPetById(petId))
                .isInstanceOf(PetNotFoundException.class);
    }

    @Test
    void getPetById_shouldCallMapperToResponse_once() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petMapper.toResponse(pet)).thenReturn(petResponse);

        getPetService.getPetById(petId);

        verify(petMapper, times(1)).toResponse(pet);
    }
}

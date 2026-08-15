package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetPhotoStoragePort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePetPhotoServiceTest {

    @Mock
    private PetRepositoryPort petRepository;

    @Mock
    private PetPhotoStoragePort photoStorage;

    @InjectMocks
    private DeletePetPhotoService deletePetPhotoService;

    private Pet petWithPhoto;
    private Pet petWithoutPhoto;
    private UUID petId;
    private UUID ownerId;
    private String photoUrl;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        photoUrl = "https://ovrunvkhvggnfkbbhcnp.supabase.co/storage/v1/object/public/pet-photos/pets/" + petId + "/profile.jpg";

        petWithPhoto = Pet.builder()
                .id(petId)
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .breed("Labrador")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .photoUrl(photoUrl)
                .createdAt(LocalDateTime.now())
                .build();

        petWithoutPhoto = petWithPhoto.toBuilder()
                .photoUrl(null)
                .build();
    }

    @Test
    void execute_shouldReturnResponse_whenPetHasPhoto() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(petWithPhoto));
        doNothing().when(photoStorage).delete(any(), any());
        when(petRepository.save(any(Pet.class))).thenReturn(petWithoutPhoto);

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .build();

        DeletePetPhotoResponse result = deletePetPhotoService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getMessage()).isEqualTo("Photo deleted successfully");
    }

    @Test
    void execute_shouldCallStorageDelete_once() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(petWithPhoto));
        doNothing().when(photoStorage).delete(any(), any());
        when(petRepository.save(any(Pet.class))).thenReturn(petWithoutPhoto);

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        deletePetPhotoService.execute(request);

        verify(photoStorage, times(1)).delete(petId, photoUrl);
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(petWithPhoto));
        doNothing().when(photoStorage).delete(any(), any());
        when(petRepository.save(any(Pet.class))).thenReturn(petWithoutPhoto);

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        deletePetPhotoService.execute(request);

        verify(petRepository, times(1)).save(any(Pet.class));
    }

    @Test
    void execute_shouldSavePetWithNullPhotoUrl() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(petWithPhoto));
        doNothing().when(photoStorage).delete(any(), any());
        when(petRepository.save(any(Pet.class))).thenReturn(petWithoutPhoto);

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        deletePetPhotoService.execute(request);

        verify(petRepository).save(argThat(pet -> pet.getPhotoUrl() == null));
    }

    @Test
    void execute_shouldThrowNoSuchElementException_whenPetNotFound() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.empty());

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        assertThatThrownBy(() -> deletePetPhotoService.execute(request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Pet not found");
    }

    @Test
    void execute_shouldNotCallStorage_whenPetNotFound() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.empty());

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        assertThatThrownBy(() -> deletePetPhotoService.execute(request))
                .isInstanceOf(NoSuchElementException.class);

        verify(photoStorage, never()).delete(any(), any());
    }

    @Test
    void execute_shouldThrowInvalidPetDataException_whenPetHasNoPhoto() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(petWithoutPhoto));

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        assertThatThrownBy(() -> deletePetPhotoService.execute(request))
                .isInstanceOf(InvalidPetDataException.class)
                .hasMessageContaining("does not have a photo");
    }

    @Test
    void execute_shouldNotCallStorage_whenPetHasNoPhoto() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(petWithoutPhoto));

        DeletePetPhotoRequest request = DeletePetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).build();

        assertThatThrownBy(() -> deletePetPhotoService.execute(request))
                .isInstanceOf(InvalidPetDataException.class);

        verify(photoStorage, never()).delete(any(), any());
    }
}
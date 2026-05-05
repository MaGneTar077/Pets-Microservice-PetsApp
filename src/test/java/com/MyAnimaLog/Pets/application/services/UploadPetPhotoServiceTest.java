package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetPhotoStoragePort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.FileSizeExceededException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidImageFormatException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadPetPhotoServiceTest {

    @Mock
    private PetRepositoryPort petRepository;

    @Mock
    private PetPhotoStoragePort photoStorage;

    @InjectMocks
    private UploadPetPhotoService uploadPetPhotoService;

    private Pet pet;
    private UUID petId;
    private UUID ownerId;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        pet = Pet.builder()
                .id(petId)
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .breed("Labrador")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        validFile = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                new byte[1024] // 1 KB
        );
    }

    @Test
    void execute_shouldReturnResponse_whenRequestIsValid() {
        String photoUrl = "https://supabase.co/storage/v1/object/public/pet-photos/pets/" + petId + "/profile.jpg";

        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(pet));
        when(photoStorage.upload(eq(petId), any())).thenReturn(photoUrl);
        when(petRepository.save(any(Pet.class))).thenReturn(pet.toBuilder().photoUrl(photoUrl).build());

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId)
                .ownerId(ownerId)
                .file(validFile)
                .build();

        UploadPetPhotoResponse result = uploadPetPhotoService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getPhotoUrl()).isEqualTo(photoUrl);
        assertThat(result.getMessage()).isEqualTo("Photo uploaded successfully");
    }

    @Test
    void execute_shouldCallFindByIdAndOwnerId_once() {
        String photoUrl = "https://supabase.co/storage/v1/object/public/pet-photos/pets/" + petId + "/profile.jpg";

        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(pet));
        when(photoStorage.upload(eq(petId), any())).thenReturn(photoUrl);
        when(petRepository.save(any(Pet.class))).thenReturn(pet.toBuilder().photoUrl(photoUrl).build());

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(validFile).build();

        uploadPetPhotoService.execute(request);

        verify(petRepository, times(1)).findByIdAndOwnerId(petId, ownerId);
    }

    @Test
    void execute_shouldCallStorageUpload_once() {
        String photoUrl = "https://supabase.co/storage/v1/object/public/pet-photos/pets/" + petId + "/profile.jpg";

        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(pet));
        when(photoStorage.upload(eq(petId), any())).thenReturn(photoUrl);
        when(petRepository.save(any(Pet.class))).thenReturn(pet.toBuilder().photoUrl(photoUrl).build());

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(validFile).build();

        uploadPetPhotoService.execute(request);

        verify(photoStorage, times(1)).upload(eq(petId), any());
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        String photoUrl = "https://supabase.co/storage/v1/object/public/pet-photos/pets/" + petId + "/profile.jpg";

        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.of(pet));
        when(photoStorage.upload(eq(petId), any())).thenReturn(photoUrl);
        when(petRepository.save(any(Pet.class))).thenReturn(pet.toBuilder().photoUrl(photoUrl).build());

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(validFile).build();

        uploadPetPhotoService.execute(request);

        verify(petRepository, times(1)).save(any(Pet.class));
    }

    @Test
    void execute_shouldThrowInvalidImageFormatException_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "profile.jpg", "image/jpeg", new byte[0]);

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(emptyFile).build();

        assertThatThrownBy(() -> uploadPetPhotoService.execute(request))
                .isInstanceOf(InvalidImageFormatException.class)
                .hasMessageContaining("File must not be empty");
    }

    @Test
    void execute_shouldThrowFileSizeExceededException_whenFileExceeds5MB() {
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "profile.jpg", "image/jpeg",
                new byte[6 * 1024 * 1024]); // 6 MB

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(bigFile).build();

        assertThatThrownBy(() -> uploadPetPhotoService.execute(request))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void execute_shouldThrowInvalidImageFormatException_whenContentTypeIsNotAllowed() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[1024]);

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(pdfFile).build();

        assertThatThrownBy(() -> uploadPetPhotoService.execute(request))
                .isInstanceOf(InvalidImageFormatException.class)
                .hasMessageContaining("Invalid image format");
    }

    @Test
    void execute_shouldThrowNoSuchElementException_whenPetNotFound() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.empty());

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(validFile).build();

        assertThatThrownBy(() -> uploadPetPhotoService.execute(request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Pet not found");
    }

    @Test
    void execute_shouldNotCallStorage_whenPetNotFound() {
        when(petRepository.findByIdAndOwnerId(petId, ownerId)).thenReturn(Optional.empty());

        UploadPetPhotoRequest request = UploadPetPhotoRequest.builder()
                .petId(petId).ownerId(ownerId).file(validFile).build();

        assertThatThrownBy(() -> uploadPetPhotoService.execute(request))
                .isInstanceOf(NoSuchElementException.class);

        verify(photoStorage, never()).upload(any(), any());
    }
}
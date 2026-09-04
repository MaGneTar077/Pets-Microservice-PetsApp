package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.in.UploadPetPhotoUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetPhotoStoragePort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.FileSizeExceededException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidImageFormatException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UploadPetPhotoService implements UploadPetPhotoUseCase {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final PetRepositoryPort petRepository;
    private final PetPhotoStoragePort photoStorage;
    private final PublishPetEventUseCase publishPetEventUseCase;

    @Override
    public UploadPetPhotoResponse execute(UploadPetPhotoRequest request) {

        validateFile(request.getFile());

        Pet pet = petRepository
                .findByIdAndOwnerId(request.getPetId(), request.getOwnerId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Pet not found or does not belong to the owner"));

        String photoUrl = photoStorage.upload(request.getPetId(), request.getFile());

        Pet updated = Pet.builder()
                .id(pet.getId())
                .ownerId(pet.getOwnerId())
                .name(pet.getName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .sex(pet.getSex())
                .birthDate(pet.getBirthDate())
                .height(pet.getHeight())
                .weight(pet.getWeight())
                .photoUrl(photoUrl)
                .createdAt(pet.getCreatedAt())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        Pet saved = petRepository.save(updated);

        publishPetEventUseCase.publish(PetEvent.builder()
                .petId(saved.getId())
                .ownerId(saved.getOwnerId())
                .petName(saved.getName())
                .eventType("PET_PHOTO_UPLOADED")
                .occurredAt(Instant.now())
                .build());

        return UploadPetPhotoResponse.builder()
                .petId(saved.getId())
                .photoUrl(saved.getPhotoUrl())
                .message("Photo uploaded successfully")
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageFormatException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeExceededException(
                    "File size exceeds the 5 MB limit. Received: " + file.getSize() + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidImageFormatException(
                    "Invalid image format. Allowed: JPEG, PNG, WEBP. Received: " + contentType);
        }
    }
}

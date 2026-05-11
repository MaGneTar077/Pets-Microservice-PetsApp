package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.UploadPetDocumentUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentStoragePort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.FileSizeExceededException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidFileException;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadPetDocumentService implements UploadPetDocumentUseCase {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final PetRepositoryPort petRepository;
    private final PetDocumentRepositoryPort documentRepository;
    private final PetDocumentStoragePort documentStorage;

    @Override
    public UploadPetDocumentResponse execute(UploadPetDocumentRequest request) {

        validateFile(request.getFile());

        petRepository.findById(request.getPetId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Pet not found with id: " + request.getPetId()));

        UUID documentId = UUID.randomUUID();

        String fileUrl = documentStorage.upload(request.getPetId(), documentId, request.getFile());

        PetDocument document = PetDocument.builder()
                .id(documentId)
                .petId(request.getPetId())
                .uploadedBy(request.getUploadedBy())
                .title(request.getTitle())
                .description(request.getDescription())
                .fileUrl(fileUrl)
                .mimeType(request.getFile().getContentType())
                .fileSizeBytes(request.getFile().getSize())
                .documentType(request.getDocumentType())
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();

        PetDocument saved = documentRepository.save(document);

        return toResponse(saved);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeExceededException(
                    "File size exceeds the 10 MB limit. Received: " + file.getSize() + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException(
                    "Invalid file format. Allowed: PDF, DOCX, DOC, JPEG, PNG, WEBP. Received: " + contentType);
        }
    }

    private UploadPetDocumentResponse toResponse(PetDocument document) {
        return UploadPetDocumentResponse.builder()
                .id(document.getId())
                .petId(document.getPetId())
                .uploadedBy(document.getUploadedBy())
                .title(document.getTitle())
                .description(document.getDescription())
                .fileUrl(document.getFileUrl())
                .mimeType(document.getMimeType())
                .fileSizeBytes(document.getFileSizeBytes())
                .documentType(document.getDocumentType())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataRequest;
import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidDocumentException;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdatePetDocumentMetadataServiceTest {

    @Mock
    private PetDocumentRepositoryPort documentRepository;

    @InjectMocks
    private UpdatePetDocumentMetadataService updatePetDocumentMetadataService;

    private PetDocument document;
    private UUID petId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        document = PetDocument.builder()
                .id(documentId)
                .petId(petId)
                .uploadedBy(UUID.randomUUID())
                .title("Vacuna rabia")
                .description("Vacuna anual")
                .fileUrl("https://supabase.co/storage/v1/object/public/pet-documents/pets/" + petId + "/documents/" + documentId + ".pdf")
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    @Test
    void execute_shouldUpdateTitle_whenOnlyTitleProvided() {
        PetDocument updated = document.toBuilder().title("Nuevo titulo").build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(PetDocument.class))).thenReturn(updated);

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(petId)
                .documentId(documentId)
                .title("Nuevo titulo")
                .build();

        UpdatePetDocumentMetadataResponse result = updatePetDocumentMetadataService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Nuevo titulo");
    }

    @Test
    void execute_shouldUpdateDescription_whenOnlyDescriptionProvided() {
        PetDocument updated = document.toBuilder().description("Nueva descripcion").build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(PetDocument.class))).thenReturn(updated);

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(petId)
                .documentId(documentId)
                .description("Nueva descripcion")
                .build();

        UpdatePetDocumentMetadataResponse result = updatePetDocumentMetadataService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Nueva descripcion");
    }

    @Test
    void execute_shouldUpdateBothFields_whenBothProvided() {
        PetDocument updated = document.toBuilder()
                .title("Nuevo titulo")
                .description("Nueva descripcion")
                .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(PetDocument.class))).thenReturn(updated);

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(petId)
                .documentId(documentId)
                .title("Nuevo titulo")
                .description("Nueva descripcion")
                .build();

        UpdatePetDocumentMetadataResponse result = updatePetDocumentMetadataService.execute(request);

        assertThat(result.getTitle()).isEqualTo("Nuevo titulo");
        assertThat(result.getDescription()).isEqualTo("Nueva descripcion");
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        PetDocument updated = document.toBuilder().title("Nuevo titulo").build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(PetDocument.class))).thenReturn(updated);

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(petId).documentId(documentId).title("Nuevo titulo").build();

        updatePetDocumentMetadataService.execute(request);

        verify(documentRepository, times(1)).save(any(PetDocument.class));
    }

    @Test
    void execute_shouldThrowDocumentNotFoundException_whenDocumentDoesNotExist() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(petId).documentId(documentId).title("Nuevo titulo").build();

        assertThatThrownBy(() -> updatePetDocumentMetadataService.execute(request))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void execute_shouldThrowDocumentNotBelongsToPetException_whenPetIdDoesNotMatch() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(UUID.randomUUID())
                .documentId(documentId)
                .title("Nuevo titulo")
                .build();

        assertThatThrownBy(() -> updatePetDocumentMetadataService.execute(request))
                .isInstanceOf(DocumentNotBelongsToPetException.class)
                .hasMessageContaining("does not belong to pet");
    }

    @Test
    void execute_shouldThrowInvalidDocumentException_whenBothFieldsAreBlank() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(petId).documentId(documentId)
                .title("").description("").build();

        assertThatThrownBy(() -> updatePetDocumentMetadataService.execute(request))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("At least one field");
    }

    @Test
    void execute_shouldThrowInvalidDocumentException_whenBothFieldsAreNull() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        UpdatePetDocumentMetadataRequest request = UpdatePetDocumentMetadataRequest.builder()
                .petId(petId).documentId(documentId).build();

        assertThatThrownBy(() -> updatePetDocumentMetadataService.execute(request))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("At least one field");
    }
}
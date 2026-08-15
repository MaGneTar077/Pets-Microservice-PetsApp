package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentStoragePort;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
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
class DeletePetDocumentServiceTest {

    @Mock
    private PetDocumentRepositoryPort documentRepository;

    @Mock
    private PetDocumentStoragePort documentStorage;

    @InjectMocks
    private DeletePetDocumentService deletePetDocumentService;

    private PetDocument document;
    private UUID petId;
    private UUID documentId;
    private UUID requestedBy;
    private String fileUrl;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        requestedBy = UUID.randomUUID();
        fileUrl = "https://supabase.co/storage/v1/object/public/pet-documents/pets/" + petId + "/documents/" + documentId + ".pdf";

        document = PetDocument.builder()
                .id(documentId)
                .petId(petId)
                .uploadedBy(requestedBy)
                .title("Vacuna rabia")
                .fileUrl(fileUrl)
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    @Test
    void execute_shouldReturnResponse_whenDocumentExists() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doNothing().when(documentStorage).delete(any(), any());
        doNothing().when(documentRepository).deleteById(any());

        DeletePetDocumentResponse result = deletePetDocumentService.execute(buildRequest());

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getMessage()).isEqualTo("Document deleted successfully");
    }

    @Test
    void execute_shouldCallStorageDelete_once() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doNothing().when(documentStorage).delete(any(), any());
        doNothing().when(documentRepository).deleteById(any());

        deletePetDocumentService.execute(buildRequest());

        verify(documentStorage, times(1)).delete(petId, fileUrl);
    }

    @Test
    void execute_shouldCallRepositoryDeleteById_once() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doNothing().when(documentStorage).delete(any(), any());
        doNothing().when(documentRepository).deleteById(any());

        deletePetDocumentService.execute(buildRequest());

        verify(documentRepository, times(1)).deleteById(documentId);
    }

    @Test
    void execute_shouldThrowDocumentNotFoundException_whenDocumentDoesNotExist() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deletePetDocumentService.execute(buildRequest()))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void execute_shouldNotCallStorage_whenDocumentNotFound() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deletePetDocumentService.execute(buildRequest()))
                .isInstanceOf(DocumentNotFoundException.class);

        verify(documentStorage, never()).delete(any(), any());
    }

    @Test
    void execute_shouldThrowDocumentNotBelongsToPetException_whenPetIdDoesNotMatch() {
        UUID differentPetId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        DeletePetDocumentRequest request = DeletePetDocumentRequest.builder()
                .petId(differentPetId)
                .documentId(documentId)
                .requestedBy(requestedBy)
                .build();

        assertThatThrownBy(() -> deletePetDocumentService.execute(request))
                .isInstanceOf(DocumentNotBelongsToPetException.class)
                .hasMessageContaining("does not belong to pet");
    }

    @Test
    void execute_shouldNotCallStorage_whenDocumentNotBelongsToPet() {
        UUID differentPetId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        DeletePetDocumentRequest request = DeletePetDocumentRequest.builder()
                .petId(differentPetId)
                .documentId(documentId)
                .requestedBy(requestedBy)
                .build();

        assertThatThrownBy(() -> deletePetDocumentService.execute(request))
                .isInstanceOf(DocumentNotBelongsToPetException.class);

        verify(documentStorage, never()).delete(any(), any());
    }

    private DeletePetDocumentRequest buildRequest() {
        return DeletePetDocumentRequest.builder()
                .petId(petId)
                .documentId(documentId)
                .requestedBy(requestedBy)
                .build();
    }
}
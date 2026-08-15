package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPetDocumentByIdServiceTest {

    @Mock
    private PetDocumentRepositoryPort documentRepository;

    @InjectMocks
    private GetPetDocumentByIdService getPetDocumentByIdService;

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
    void execute_shouldReturnDocument_whenDocumentExists() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        GetPetDocumentResponse result = getPetDocumentByIdService.execute(petId, documentId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(documentId);
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getTitle()).isEqualTo("Vacuna rabia");
        assertThat(result.getActive()).isTrue();
    }

    @Test
    void execute_shouldCallFindById_once() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        getPetDocumentByIdService.execute(petId, documentId);

        verify(documentRepository, times(1)).findById(documentId);
    }

    @Test
    void execute_shouldThrowDocumentNotFoundException_whenDocumentDoesNotExist() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getPetDocumentByIdService.execute(petId, documentId))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void execute_shouldThrowDocumentNotBelongsToPetException_whenPetIdDoesNotMatch() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        UUID differentPetId = UUID.randomUUID();

        assertThatThrownBy(() -> getPetDocumentByIdService.execute(differentPetId, documentId))
                .isInstanceOf(DocumentNotBelongsToPetException.class)
                .hasMessageContaining("does not belong to pet");
    }
}
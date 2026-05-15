package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPetDocumentsServiceTest {

    @Mock
    private PetRepositoryPort petRepository;

    @Mock
    private PetDocumentRepositoryPort petDocumentRepository;

    @InjectMocks
    private ListPetDocumentsService listPetDocumentsService;

    private Pet pet;
    private PetDocument document1;
    private PetDocument document2;
    private UUID petId;
    private String fileUrl;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        fileUrl = "https://supabase.co/storage/v1/object/public/pet-documents/pets/" + petId + "/documents/";

        pet = Pet.builder()
                .id(petId)
                .ownerId(UUID.randomUUID())
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        document1 = PetDocument.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .uploadedBy(UUID.randomUUID())
                .title("Vacuna rabia")
                .description("Vacuna anual")
                .fileUrl(fileUrl + "doc1.pdf")
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();

        document2 = PetDocument.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .uploadedBy(UUID.randomUUID())
                .title("Resultado laboratorio")
                .description("Examen de sangre")
                .fileUrl(fileUrl + "doc2.pdf")
                .mimeType("application/pdf")
                .fileSizeBytes(2048L)
                .documentType(DocumentType.LAB_RESULT)
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    @Test
    void execute_shouldReturnListOfDocuments_whenPetExists() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petDocumentRepository.findAllByPetId(petId)).thenReturn(List.of(document1, document2));

        List<PetDocumentSummaryResponse> result = listPetDocumentsService.execute(petId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Vacuna rabia");
        assertThat(result.get(1).getTitle()).isEqualTo("Resultado laboratorio");
    }

    @Test
    void execute_shouldReturnEmptyList_whenPetHasNoDocuments() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petDocumentRepository.findAllByPetId(petId)).thenReturn(List.of());

        List<PetDocumentSummaryResponse> result = listPetDocumentsService.execute(petId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void execute_shouldReturnFileUrl_inEachDocument() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petDocumentRepository.findAllByPetId(petId)).thenReturn(List.of(document1, document2));

        List<PetDocumentSummaryResponse> result = listPetDocumentsService.execute(petId);

        assertThat(result.get(0).getFileUrl()).isEqualTo(fileUrl + "doc1.pdf");
        assertThat(result.get(1).getFileUrl()).isEqualTo(fileUrl + "doc2.pdf");
    }

    @Test
    void execute_shouldCallFindAllByPetId_once() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petDocumentRepository.findAllByPetId(petId)).thenReturn(List.of(document1));

        listPetDocumentsService.execute(petId);

        verify(petDocumentRepository, times(1)).findAllByPetId(petId);
    }

    @Test
    void execute_shouldCallFindById_once() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petDocumentRepository.findAllByPetId(petId)).thenReturn(List.of());

        listPetDocumentsService.execute(petId);

        verify(petRepository, times(1)).findById(petId);
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listPetDocumentsService.execute(petId))
                .isInstanceOf(PetNotFoundException.class)
                .hasMessageContaining("Pet not found");
    }

    @Test
    void execute_shouldNotCallDocumentRepository_whenPetNotFound() {
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listPetDocumentsService.execute(petId))
                .isInstanceOf(PetNotFoundException.class);

        verify(petDocumentRepository, never()).findAllByPetId(any());
    }
}
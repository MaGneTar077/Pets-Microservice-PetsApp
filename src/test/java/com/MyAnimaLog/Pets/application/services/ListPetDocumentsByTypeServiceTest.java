package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPetDocumentsByTypeServiceTest {

    @Mock
    private PetDocumentRepositoryPort petDocumentRepository;

    @Mock
    private PetAccessPolicy petAccessPolicy;

    @InjectMocks
    private ListPetDocumentsByTypeService listPetDocumentsByTypeService;

    private Pet pet;
    private PetDocument vaccineDoc;
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
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        vaccineDoc = PetDocument.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .uploadedBy(UUID.randomUUID())
                .title("Vacuna rabia")
                .description("Vacuna anual")
                .fileUrl("https://supabase.co/storage/v1/object/public/pet-documents/pets/" + petId + "/documents/doc1.pdf")
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    @Test
    void execute_shouldReturnOnlyVaccineDocuments_whenCallerIsTheOwner() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentRepository.findAllByPetIdAndDocumentType(petId, DocumentType.VACCINE))
                .thenReturn(List.of(vaccineDoc));

        List<PetDocumentSummaryResponse> result =
                listPetDocumentsByTypeService.execute(petId, DocumentType.VACCINE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDocumentType()).isEqualTo(DocumentType.VACCINE);
        assertThat(result.get(0).getTitle()).isEqualTo("Vacuna rabia");
    }

    @Test
    void execute_shouldReturnDocuments_whenCallerIsAnEditor() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentRepository.findAllByPetIdAndDocumentType(petId, DocumentType.VACCINE))
                .thenReturn(List.of(vaccineDoc));

        assertThat(listPetDocumentsByTypeService.execute(petId, DocumentType.VACCINE)).hasSize(1);
    }

    @Test
    void execute_shouldReturnDocuments_whenCallerIsAViewer() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentRepository.findAllByPetIdAndDocumentType(petId, DocumentType.VACCINE))
                .thenReturn(List.of(vaccineDoc));

        assertThat(listPetDocumentsByTypeService.execute(petId, DocumentType.VACCINE)).hasSize(1);
    }

    @Test
    void execute_shouldThrow403_whenCallerHasNoAccess() {
        when(petAccessPolicy.requireRead(petId)).thenThrow(new UnauthorizedPetAccessException());

        assertThatThrownBy(() -> listPetDocumentsByTypeService.execute(petId, DocumentType.VACCINE))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verifyNoInteractions(petDocumentRepository);
    }

    @Test
    void execute_shouldReturnEmptyList_whenNoDocumentsMatchType() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentRepository.findAllByPetIdAndDocumentType(petId, DocumentType.SURGERY_REPORT))
                .thenReturn(List.of());

        List<PetDocumentSummaryResponse> result =
                listPetDocumentsByTypeService.execute(petId, DocumentType.SURGERY_REPORT);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_shouldCallFindAllByPetIdAndDocumentType_once() {
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentRepository.findAllByPetIdAndDocumentType(petId, DocumentType.VACCINE))
                .thenReturn(List.of(vaccineDoc));

        listPetDocumentsByTypeService.execute(petId, DocumentType.VACCINE);

        verify(petDocumentRepository, times(1))
                .findAllByPetIdAndDocumentType(petId, DocumentType.VACCINE);
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetDoesNotExist() {
        when(petAccessPolicy.requireRead(petId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() ->
                listPetDocumentsByTypeService.execute(petId, DocumentType.VACCINE))
                .isInstanceOf(PetNotFoundException.class);
    }

    @Test
    void execute_shouldNotCallDocumentRepository_whenPetNotFound() {
        when(petAccessPolicy.requireRead(petId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() ->
                listPetDocumentsByTypeService.execute(petId, DocumentType.VACCINE))
                .isInstanceOf(PetNotFoundException.class);

        verify(petDocumentRepository, never()).findAllByPetIdAndDocumentType(any(), any());
    }
}

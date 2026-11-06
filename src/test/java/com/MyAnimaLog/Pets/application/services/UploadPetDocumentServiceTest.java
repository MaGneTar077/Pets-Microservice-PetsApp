package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentStoragePort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.FileSizeExceededException;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidFileException;
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
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadPetDocumentServiceTest {

    @Mock
    private PetDocumentRepositoryPort documentRepository;

    @Mock
    private PetDocumentStoragePort documentStorage;

    @Mock
    private PublishPetEventUseCase publishPetEventUseCase;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @Mock
    private PetAccessPolicy petAccessPolicy;

    @InjectMocks
    private UploadPetDocumentService uploadPetDocumentService;

    private Pet pet;
    private PetDocument savedDocument;
    private UUID petId;
    private UUID uploadedBy;
    private String fileUrl;
    private MockMultipartFile validPdf;
    private MockMultipartFile validImage;
    private MockMultipartFile validDocx;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();
        fileUrl = "https://supabase.co/storage/v1/object/public/pet-documents/pets/" + petId + "/documents/doc.pdf";

        lenient().when(currentUserIdentity.resolveUserId(any())).thenReturn(uploadedBy);

        pet = Pet.builder()
                .id(petId)
                .ownerId(uploadedBy)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .createdAt(LocalDateTime.now())
                .build();

        savedDocument = PetDocument.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .uploadedBy(uploadedBy)
                .title("Vacuna rabia")
                .description("Vacuna anual")
                .fileUrl(fileUrl)
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();

        validPdf = new MockMultipartFile(
                "file", "vacuna.pdf", "application/pdf", new byte[1024]);

        validImage = new MockMultipartFile(
                "file", "resultado.jpg", "image/jpeg", new byte[1024]);

        validDocx = new MockMultipartFile(
                "file", "historial.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[1024]);
    }

    @Test
    void execute_shouldReturnResponse_whenCallerIsTheOwner() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        UploadPetDocumentRequest request = buildRequest(validPdf);
        UploadPetDocumentResponse result = uploadPetDocumentService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getFileUrl()).isEqualTo(fileUrl);
    }

    @Test
    void execute_shouldReturnResponse_whenCallerIsAnEditor() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        assertThat(uploadPetDocumentService.execute(buildRequest(validPdf))).isNotNull();
    }

    @Test
    void execute_shouldThrow403_whenCallerIsAViewer() {
        when(petAccessPolicy.requireWrite(petId)).thenThrow(new UnauthorizedPetAccessException());

        assertThatThrownBy(() -> uploadPetDocumentService.execute(buildRequest(validPdf)))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verifyNoInteractions(documentStorage);
    }

    @Test
    void execute_shouldThrow403_whenCallerHasNoAccess() {
        when(petAccessPolicy.requireWrite(petId)).thenThrow(new UnauthorizedPetAccessException());

        assertThatThrownBy(() -> uploadPetDocumentService.execute(buildRequest(validPdf)))
                .isInstanceOf(UnauthorizedPetAccessException.class);
    }

    @Test
    void execute_shouldThrow403_whenUploadedByIsUnrelatedToTheToken() {
        UUID unrelatedUser = UUID.randomUUID();
        when(currentUserIdentity.resolveUserId(unrelatedUser)).thenThrow(new IdentityMismatchException());

        UploadPetDocumentRequest request = UploadPetDocumentRequest.builder()
                .petId(petId).uploadedBy(unrelatedUser).title("Vacuna rabia")
                .documentType(DocumentType.VACCINE).file(validPdf).build();

        assertThatThrownBy(() -> uploadPetDocumentService.execute(request))
                .isInstanceOf(IdentityMismatchException.class);

        verifyNoInteractions(petAccessPolicy, documentStorage);
    }

    @Test
    void execute_shouldUseTheTokenIdentity_asUploadedBy() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        uploadPetDocumentService.execute(buildRequest(validPdf));

        verify(documentRepository).save(argThat(doc -> doc.getUploadedBy().equals(uploadedBy)));
    }

    @Test
    void execute_shouldReturnResponse_whenImageIsValid() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        UploadPetDocumentRequest request = buildRequest(validImage);
        UploadPetDocumentResponse result = uploadPetDocumentService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
    }

    @Test
    void execute_shouldReturnResponse_whenDocxIsValid() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        UploadPetDocumentRequest request = buildRequest(validDocx);
        UploadPetDocumentResponse result = uploadPetDocumentService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.getPetId()).isEqualTo(petId);
    }

    @Test
    void execute_shouldCallStorageUpload_once() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        uploadPetDocumentService.execute(buildRequest(validPdf));

        verify(documentStorage, times(1)).upload(any(), any(), any());
    }

    @Test
    void execute_shouldCallRepositorySave_once() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        uploadPetDocumentService.execute(buildRequest(validPdf));

        verify(documentRepository, times(1)).save(any(PetDocument.class));
    }

    @Test
    void execute_shouldSaveDocumentWithActiveTrue() {
        when(petAccessPolicy.requireWrite(petId)).thenReturn(pet);
        when(documentStorage.upload(any(), any(), any())).thenReturn(fileUrl);
        when(documentRepository.save(any(PetDocument.class))).thenReturn(savedDocument);

        uploadPetDocumentService.execute(buildRequest(validPdf));

        verify(documentRepository).save(argThat(doc -> doc.getActive().equals(true)));
    }

    @Test
    void execute_shouldThrowInvalidFileException_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> uploadPetDocumentService.execute(buildRequest(emptyFile)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File must not be empty");
    }

    @Test
    void execute_shouldThrowFileSizeExceededException_whenFileExceeds10MB() {
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf",
                new byte[11 * 1024 * 1024]); // 11 MB

        assertThatThrownBy(() -> uploadPetDocumentService.execute(buildRequest(bigFile)))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void execute_shouldThrowInvalidFileException_whenContentTypeIsNotAllowed() {
        MockMultipartFile excelFile = new MockMultipartFile(
                "file", "datos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[1024]);

        assertThatThrownBy(() -> uploadPetDocumentService.execute(buildRequest(excelFile)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid file format");
    }

    @Test
    void execute_shouldThrowPetNotFoundException_whenPetNotFound() {
        when(petAccessPolicy.requireWrite(petId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() -> uploadPetDocumentService.execute(buildRequest(validPdf)))
                .isInstanceOf(PetNotFoundException.class);
    }

    @Test
    void execute_shouldNotCallStorage_whenPetNotFound() {
        when(petAccessPolicy.requireWrite(petId)).thenThrow(new PetNotFoundException(petId));

        assertThatThrownBy(() -> uploadPetDocumentService.execute(buildRequest(validPdf)))
                .isInstanceOf(PetNotFoundException.class);

        verify(documentStorage, never()).upload(any(), any(), any());
    }

    private UploadPetDocumentRequest buildRequest(MockMultipartFile file) {
        return UploadPetDocumentRequest.builder()
                .petId(petId)
                .uploadedBy(uploadedBy)
                .title("Vacuna rabia")
                .description("Vacuna anual")
                .documentType(DocumentType.VACCINE)
                .file(file)
                .build();
    }
}

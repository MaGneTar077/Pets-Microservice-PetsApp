package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlRequest;
import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetDocumentStoragePort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.application.security.PetAccessPolicy;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
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
class GetDownloadUrlServiceTest {

    @Mock
    private PetDocumentRepositoryPort petDocumentRepositoryPort;

    @Mock
    private PetDocumentStoragePort petDocumentStoragePort;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @Mock
    private PetAccessPolicy petAccessPolicy;

    @InjectMocks
    private GetDownloadUrlService getDownloadUrlService;

    private UUID petId;
    private UUID documentId;
    private UUID requestedBy;
    private String fileUrl;
    private PetDocument document;
    private Pet pet;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        requestedBy = UUID.randomUUID();
        fileUrl = "https://supabase.co/storage/v1/object/public/pet-documents/pets/" + petId + "/documents/" + documentId + ".pdf";

        lenient().when(currentUserIdentity.resolveUserId(any())).thenReturn(requestedBy);

        document = PetDocument.builder()
                .id(documentId)
                .petId(petId)
                .uploadedBy(requestedBy)
                .title("Vacuna rabia")
                .description("Vacuna anual")
                .fileUrl(fileUrl)
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .active(true)
                .build();

        pet = Pet.builder()
                .id(petId)
                .ownerId(UUID.randomUUID())
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .build();
    }

    @Test
    void getDownloadUrl_shouldReturnResponse_whenCallerIsTheOwner() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentStoragePort.getDownloadUrl(fileUrl)).thenReturn(fileUrl);

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId)
                .documentId(documentId)
                .requestedBy(requestedBy)
                .build();

        GetDownloadUrlResponse result = getDownloadUrlService.getDownloadUrl(request);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getPetId()).isEqualTo(petId);
        assertThat(result.getTitle()).isEqualTo("Vacuna rabia");
        assertThat(result.getDownloadUrl()).isEqualTo(fileUrl);
        assertThat(result.getMimeType()).isEqualTo("application/pdf");
        assertThat(result.getFileSizeBytes()).isEqualTo(1024L);
    }

    @Test
    void getDownloadUrl_shouldReturnResponse_whenCallerIsAnEditor() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentStoragePort.getDownloadUrl(fileUrl)).thenReturn(fileUrl);

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        assertThat(getDownloadUrlService.getDownloadUrl(request)).isNotNull();
    }

    @Test
    void getDownloadUrl_shouldReturnResponse_whenCallerIsAViewer() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentStoragePort.getDownloadUrl(fileUrl)).thenReturn(fileUrl);

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        assertThat(getDownloadUrlService.getDownloadUrl(request)).isNotNull();
    }

    @Test
    void getDownloadUrl_shouldThrow403_whenCallerHasNoAccess() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(petAccessPolicy.requireRead(petId)).thenThrow(new UnauthorizedPetAccessException());

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        assertThatThrownBy(() -> getDownloadUrlService.getDownloadUrl(request))
                .isInstanceOf(UnauthorizedPetAccessException.class);

        verify(petDocumentStoragePort, never()).getDownloadUrl(any());
    }

    @Test
    void getDownloadUrl_shouldThrow403_whenRequestedByIsUnrelatedToTheToken() {
        UUID unrelatedUser = UUID.randomUUID();
        when(currentUserIdentity.resolveUserId(unrelatedUser)).thenThrow(new IdentityMismatchException());

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(unrelatedUser).build();

        assertThatThrownBy(() -> getDownloadUrlService.getDownloadUrl(request))
                .isInstanceOf(IdentityMismatchException.class);

        verifyNoInteractions(petDocumentRepositoryPort, petDocumentStoragePort);
    }

    @Test
    void getDownloadUrl_shouldCallRepositoryFindById_once() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentStoragePort.getDownloadUrl(fileUrl)).thenReturn(fileUrl);

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        getDownloadUrlService.getDownloadUrl(request);

        verify(petDocumentRepositoryPort, times(1)).findById(documentId);
    }

    @Test
    void getDownloadUrl_shouldCallStorageGetDownloadUrl_once() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(petAccessPolicy.requireRead(petId)).thenReturn(pet);
        when(petDocumentStoragePort.getDownloadUrl(fileUrl)).thenReturn(fileUrl);

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        getDownloadUrlService.getDownloadUrl(request);

        verify(petDocumentStoragePort, times(1)).getDownloadUrl(fileUrl);
    }

    @Test
    void getDownloadUrl_shouldThrowDocumentNotFoundException_whenDocumentDoesNotExist() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        assertThatThrownBy(() -> getDownloadUrlService.getDownloadUrl(request))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void getDownloadUrl_shouldNotCallStorage_whenDocumentNotFound() {
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        assertThatThrownBy(() -> getDownloadUrlService.getDownloadUrl(request))
                .isInstanceOf(DocumentNotFoundException.class);

        verify(petDocumentStoragePort, never()).getDownloadUrl(any());
    }

    @Test
    void getDownloadUrl_shouldThrowDocumentNotBelongsToPetException_whenPetIdMismatch() {
        UUID otherPetId = UUID.randomUUID();
        PetDocument documentOtherPet = document.toBuilder().petId(otherPetId).build();
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(documentOtherPet));

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        assertThatThrownBy(() -> getDownloadUrlService.getDownloadUrl(request))
                .isInstanceOf(DocumentNotBelongsToPetException.class);
    }

    @Test
    void getDownloadUrl_shouldNotCallStorage_whenPetIdMismatch() {
        UUID otherPetId = UUID.randomUUID();
        PetDocument documentOtherPet = document.toBuilder().petId(otherPetId).build();
        when(petDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(documentOtherPet));

        GetDownloadUrlRequest request = GetDownloadUrlRequest.builder()
                .petId(petId).documentId(documentId).requestedBy(requestedBy).build();

        assertThatThrownBy(() -> getDownloadUrlService.getDownloadUrl(request))
                .isInstanceOf(DocumentNotBelongsToPetException.class);

        verify(petDocumentStoragePort, never()).getDownloadUrl(any());
    }
}

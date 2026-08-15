package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.UploadPetDocumentUseCase;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.exceptions.FileSizeExceededException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidFileException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UploadPetDocumentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class UploadPetDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadPetDocumentUseCase uploadPetDocumentUseCase;

    private UUID petId;
    private UUID uploadedBy;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();

        validFile = new MockMultipartFile(
                "file", "vacuna.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[1024]);
    }

    @Test
    void uploadDocument_shouldReturn201_whenRequestIsValid() throws Exception {
        UploadPetDocumentResponse response = UploadPetDocumentResponse.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .uploadedBy(uploadedBy)
                .title("Vacuna rabia")
                .fileUrl("https://supabase.co/storage/v1/object/public/pet-documents/pets/" + petId + "/documents/doc.pdf")
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(uploadPetDocumentUseCase.execute(any(UploadPetDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(multipart("/pets/{petId}/documents", petId)
                        .file(validFile)
                        .param("uploadedBy", uploadedBy.toString())
                        .param("title", "Vacuna rabia")
                        .param("description", "Vacuna anual")
                        .param("documentType", "VACCINE")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.documentType").value("VACCINE"))
                .andExpect(jsonPath("$.fileUrl").isNotEmpty());
    }

    @Test
    void uploadDocument_shouldReturn404_whenPetNotFound() throws Exception {
        when(uploadPetDocumentUseCase.execute(any(UploadPetDocumentRequest.class)))
                .thenThrow(new NoSuchElementException("Pet not found"));

        mockMvc.perform(multipart("/pets/{petId}/documents", petId)
                        .file(validFile)
                        .param("uploadedBy", uploadedBy.toString())
                        .param("title", "Vacuna rabia")
                        .param("documentType", "VACCINE")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadDocument_shouldReturn415_whenFileFormatIsInvalid() throws Exception {
        when(uploadPetDocumentUseCase.execute(any(UploadPetDocumentRequest.class)))
                .thenThrow(new InvalidFileException("Invalid file format"));

        mockMvc.perform(multipart("/pets/{petId}/documents", petId)
                        .file(validFile)
                        .param("uploadedBy", uploadedBy.toString())
                        .param("title", "Vacuna rabia")
                        .param("documentType", "VACCINE")
                        .with(csrf()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void uploadDocument_shouldReturn413_whenFileSizeExceeded() throws Exception {
        when(uploadPetDocumentUseCase.execute(any(UploadPetDocumentRequest.class)))
                .thenThrow(new FileSizeExceededException("File size exceeds the 10 MB limit"));

        mockMvc.perform(multipart("/pets/{petId}/documents", petId)
                        .file(validFile)
                        .param("uploadedBy", uploadedBy.toString())
                        .param("title", "Vacuna rabia")
                        .param("documentType", "VACCINE")
                        .with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }
}
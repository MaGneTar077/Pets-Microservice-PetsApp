package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataRequest;
import com.MyAnimaLog.Pets.application.dto.UpdatePetDocumentMetadataResponse;
import com.MyAnimaLog.Pets.application.ports.in.UpdatePetDocumentMetadataUseCase;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidDocumentException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UpdatePetDocumentMetadataController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class UpdatePetDocumentMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdatePetDocumentMetadataUseCase updatePetDocumentMetadataUseCase;

    private UUID petId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Test
    void updateMetadata_shouldReturn200_whenRequestIsValid() throws Exception {
        UpdatePetDocumentMetadataResponse response = UpdatePetDocumentMetadataResponse.builder()
                .id(documentId)
                .petId(petId)
                .title("Nuevo titulo")
                .description("Nueva descripcion")
                .documentType(DocumentType.VACCINE)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(updatePetDocumentMetadataUseCase.execute(any(UpdatePetDocumentMetadataRequest.class)))
                .thenReturn(response);

        UpdatePetDocumentMetadataRequest body = UpdatePetDocumentMetadataRequest.builder()
                .title("Nuevo titulo")
                .description("Nueva descripcion")
                .build();

        mockMvc.perform(patch("/pets/{petId}/documents/{documentId}/metadata", petId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo titulo"))
                .andExpect(jsonPath("$.description").value("Nueva descripcion"));
    }

    @Test
    void updateMetadata_shouldReturn404_whenDocumentNotFound() throws Exception {
        when(updatePetDocumentMetadataUseCase.execute(any(UpdatePetDocumentMetadataRequest.class)))
                .thenThrow(new DocumentNotFoundException("Document not found"));

        UpdatePetDocumentMetadataRequest body = UpdatePetDocumentMetadataRequest.builder()
                .title("Nuevo titulo").build();

        mockMvc.perform(patch("/pets/{petId}/documents/{documentId}/metadata", petId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMetadata_shouldReturn403_whenDocumentNotBelongsToPet() throws Exception {
        when(updatePetDocumentMetadataUseCase.execute(any(UpdatePetDocumentMetadataRequest.class)))
                .thenThrow(new DocumentNotBelongsToPetException("Document does not belong to pet"));

        UpdatePetDocumentMetadataRequest body = UpdatePetDocumentMetadataRequest.builder()
                .title("Nuevo titulo").build();

        mockMvc.perform(patch("/pets/{petId}/documents/{documentId}/metadata", petId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMetadata_shouldReturn400_whenBothFieldsAreBlank() throws Exception {
        when(updatePetDocumentMetadataUseCase.execute(any(UpdatePetDocumentMetadataRequest.class)))
                .thenThrow(new InvalidDocumentException("At least one field must be provided"));

        UpdatePetDocumentMetadataRequest body = UpdatePetDocumentMetadataRequest.builder()
                .title("").description("").build();

        mockMvc.perform(patch("/pets/{petId}/documents/{documentId}/metadata", petId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
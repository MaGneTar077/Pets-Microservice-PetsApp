package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetDocumentUseCase;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotBelongsToPetException;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentNotFoundException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DeletePetDocumentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class DeletePetDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeletePetDocumentUseCase deletePetDocumentUseCase;

    private UUID petId;
    private UUID documentId;
    private UUID requestedBy;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        requestedBy = UUID.randomUUID();
    }

    @Test
    void deleteDocument_shouldReturn200_whenDocumentExists() throws Exception {
        DeletePetDocumentResponse response = DeletePetDocumentResponse.builder()
                .documentId(documentId)
                .message("Document deleted successfully")
                .build();

        when(deletePetDocumentUseCase.execute(any(DeletePetDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(delete("/pets/{petId}/documents/{documentId}", petId, documentId)
                        .param("requestedBy", requestedBy.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.message").value("Document deleted successfully"));
    }

    @Test
    void deleteDocument_shouldReturn404_whenDocumentNotFound() throws Exception {
        when(deletePetDocumentUseCase.execute(any(DeletePetDocumentRequest.class)))
                .thenThrow(new DocumentNotFoundException("Document not found"));

        mockMvc.perform(delete("/pets/{petId}/documents/{documentId}", petId, documentId)
                        .param("requestedBy", requestedBy.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocument_shouldReturn403_whenDocumentNotBelongsToPet() throws Exception {
        when(deletePetDocumentUseCase.execute(any(DeletePetDocumentRequest.class)))
                .thenThrow(new DocumentNotBelongsToPetException("Document does not belong to pet"));

        mockMvc.perform(delete("/pets/{petId}/documents/{documentId}", petId, documentId)
                        .param("requestedBy", requestedBy.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
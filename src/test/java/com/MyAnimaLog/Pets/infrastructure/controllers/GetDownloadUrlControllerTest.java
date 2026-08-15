package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlRequest;
import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetDownloadUrlUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetDownloadUrlController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetDownloadUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDownloadUrlUseCase getDownloadUrlUseCase;

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
    }

    @Test
    void getDownloadUrl_shouldReturn200_whenRequestIsValid() throws Exception {
        GetDownloadUrlResponse response = GetDownloadUrlResponse.builder()
                .documentId(documentId)
                .petId(petId)
                .title("Vacuna rabia")
                .downloadUrl(fileUrl)
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .build();

        when(getDownloadUrlUseCase.getDownloadUrl(any(GetDownloadUrlRequest.class))).thenReturn(response);

        mockMvc.perform(get("/pets/{petId}/documents/{documentId}/download-url", petId, documentId)
                        .param("requestedBy", requestedBy.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.title").value("Vacuna rabia"))
                .andExpect(jsonPath("$.downloadUrl").value(fileUrl))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSizeBytes").value(1024));
    }

    @Test
    void getDownloadUrl_shouldReturn404_whenDocumentNotFound() throws Exception {
        when(getDownloadUrlUseCase.getDownloadUrl(any(GetDownloadUrlRequest.class)))
                .thenThrow(new DocumentNotFoundException(documentId));

        mockMvc.perform(get("/pets/{petId}/documents/{documentId}/download-url", petId, documentId)
                        .param("requestedBy", requestedBy.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getDownloadUrl_shouldReturn403_whenDocumentNotBelongsToPet() throws Exception {
        when(getDownloadUrlUseCase.getDownloadUrl(any(GetDownloadUrlRequest.class)))
                .thenThrow(new DocumentNotBelongsToPetException(documentId, petId));

        mockMvc.perform(get("/pets/{petId}/documents/{documentId}/download-url", petId, documentId)
                        .param("requestedBy", requestedBy.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }
}
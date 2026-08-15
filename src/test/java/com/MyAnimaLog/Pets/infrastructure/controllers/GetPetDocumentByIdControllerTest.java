package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPetDocumentResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetDocumentByIdUseCase;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetPetDocumentByIdController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetPetDocumentByIdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPetDocumentByIdUseCase getPetDocumentByIdUseCase;

    private UUID petId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Test
    void getDocumentById_shouldReturn200_whenDocumentExists() throws Exception {
        GetPetDocumentResponse response = GetPetDocumentResponse.builder()
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

        when(getPetDocumentByIdUseCase.execute(petId, documentId)).thenReturn(response);

        mockMvc.perform(get("/pets/{petId}/documents/{documentId}", petId, documentId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.title").value("Vacuna rabia"))
                .andExpect(jsonPath("$.documentType").value("VACCINE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getDocumentById_shouldReturn404_whenDocumentNotFound() throws Exception {
        when(getPetDocumentByIdUseCase.execute(petId, documentId))
                .thenThrow(new DocumentNotFoundException("Document not found"));

        mockMvc.perform(get("/pets/{petId}/documents/{documentId}", petId, documentId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDocumentById_shouldReturn403_whenDocumentNotBelongsToPet() throws Exception {
        when(getPetDocumentByIdUseCase.execute(petId, documentId))
                .thenThrow(new DocumentNotBelongsToPetException("Document does not belong to pet"));

        mockMvc.perform(get("/pets/{petId}/documents/{documentId}", petId, documentId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
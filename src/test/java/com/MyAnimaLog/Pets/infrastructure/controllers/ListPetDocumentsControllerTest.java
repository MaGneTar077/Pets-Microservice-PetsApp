package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.application.ports.in.ListPetDocumentsUseCase;
import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ListPetDocumentsController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class ListPetDocumentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPetDocumentsUseCase listPetDocumentsUseCase;

    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
    }

    @Test
    void listDocuments_shouldReturn200_withListOfDocuments() throws Exception {
        List<PetDocumentSummaryResponse> response = List.of(
                PetDocumentSummaryResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Vacuna rabia")
                        .description("Vacuna anual")
                        .fileUrl("https://supabase.co/storage/pet-documents/doc1.pdf")
                        .mimeType("application/pdf")
                        .fileSizeBytes(1024L)
                        .documentType(DocumentType.VACCINE)
                        .uploadedAt(LocalDateTime.now())
                        .build(),
                PetDocumentSummaryResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Resultado laboratorio")
                        .description("Examen de sangre")
                        .fileUrl("https://supabase.co/storage/pet-documents/doc2.pdf")
                        .mimeType("application/pdf")
                        .fileSizeBytes(2048L)
                        .documentType(DocumentType.LAB_RESULT)
                        .uploadedAt(LocalDateTime.now())
                        .build()
        );

        when(listPetDocumentsUseCase.execute(petId)).thenReturn(response);

        mockMvc.perform(get("/pets/{petId}/documents", petId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Vacuna rabia"))
                .andExpect(jsonPath("$[1].title").value("Resultado laboratorio"))
                .andExpect(jsonPath("$[0].fileUrl").isNotEmpty())
                .andExpect(jsonPath("$[1].fileUrl").isNotEmpty());
    }

    @Test
    void listDocuments_shouldReturn200_withEmptyList_whenPetHasNoDocuments() throws Exception {
        when(listPetDocumentsUseCase.execute(petId)).thenReturn(List.of());

        mockMvc.perform(get("/pets/{petId}/documents", petId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listDocuments_shouldReturn404_whenPetNotFound() throws Exception {
        when(listPetDocumentsUseCase.execute(petId))
                .thenThrow(new PetNotFoundException("Pet not found"));

        mockMvc.perform(get("/pets/{petId}/documents", petId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
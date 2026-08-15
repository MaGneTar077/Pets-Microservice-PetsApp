package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetDocumentSummaryResponse;
import com.MyAnimaLog.Pets.application.ports.in.ListPetDocumentsByTypeUseCase;
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
        controllers = ListPetDocumentsByTypeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class ListPetDocumentsByTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPetDocumentsByTypeUseCase listPetDocumentsByTypeUseCase;

    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
    }

    @Test
    void listDocumentsByType_shouldReturn200_withMatchingDocuments() throws Exception {
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
                        .build()
        );

        when(listPetDocumentsByTypeUseCase.execute(petId, DocumentType.VACCINE))
                .thenReturn(response);

        mockMvc.perform(get("/pets/{petId}/documents/type/{documentType}", petId, "VACCINE")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Vacuna rabia"))
                .andExpect(jsonPath("$[0].documentType").value("VACCINE"));
    }

    @Test
    void listDocumentsByType_shouldReturn200_withEmptyList_whenNoDocumentsMatchType() throws Exception {
        when(listPetDocumentsByTypeUseCase.execute(petId, DocumentType.SURGERY_REPORT))
                .thenReturn(List.of());

        mockMvc.perform(get("/pets/{petId}/documents/type/{documentType}", petId, "SURGERY_REPORT")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listDocumentsByType_shouldReturn404_whenPetNotFound() throws Exception {
        when(listPetDocumentsByTypeUseCase.execute(petId, DocumentType.VACCINE))
                .thenThrow(new PetNotFoundException("Pet not found"));

        mockMvc.perform(get("/pets/{petId}/documents/type/{documentType}", petId, "VACCINE")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.EditPetRequest;
import com.MyAnimaLog.Pets.application.dto.EditPetResponse;
import com.MyAnimaLog.Pets.application.ports.in.EditPetUseCase;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
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
        controllers = EditPetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@WithMockUser
class EditPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EditPetUseCase editPetUseCase;

    private ObjectMapper objectMapper;
    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    @Test
    void editPet_shouldReturn200_whenRequestIsValid() throws Exception {

        EditPetResponse response = EditPetResponse.builder()
                .id(petId)
                .ownerId(ownerId)
                .name("Firulais Editado")
                .sex(Sex.MALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(editPetUseCase.editPet(any(), any(), any())).thenReturn(response);

        EditPetRequest request = new EditPetRequest();
        request.setName("Firulais Editado");

        mockMvc.perform(patch("/pets/{petId}", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Firulais Editado"));
    }

    @Test
    void editPet_shouldReturn400_whenInvalidData() throws Exception {

        EditPetRequest request = new EditPetRequest();
        request.setWeight(-5.0);

        when(editPetUseCase.editPet(any(), any(), any()))
                .thenThrow(new InvalidPetDataException("Invalid weight"));

        mockMvc.perform(patch("/pets/{petId}", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
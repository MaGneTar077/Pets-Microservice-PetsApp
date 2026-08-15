package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AddPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.services.PetService;
import com.MyAnimaLog.Pets.domain.enums.Sex;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AddPetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@WithMockUser
class AddPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetService petService;

    private ObjectMapper objectMapper;
    private AddPetRequest request;
    private PetResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        UUID ownerId = UUID.randomUUID();

        request = new AddPetRequest();
        request.setOwnerId(ownerId);
        request.setName("Firulais");
        request.setSpecies("Perro");
        request.setBreed("Labrador");
        request.setSex(Sex.MALE);
        request.setBirthDate(LocalDate.of(2022, 1, 15));
        request.setHeight(45.5);
        request.setWeight(12.3);

        response = PetResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Firulais")
                .species("Perro")
                .breed("Labrador")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .height(45.5)
                .weight(12.3)
                .photoUrl(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addPet_shouldReturn201_whenRequestIsValid() throws Exception {
        when(petService.add(any(AddPetRequest.class))).thenReturn(response);

        mockMvc.perform(post("/pets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Firulais"))
                .andExpect(jsonPath("$.species").value("Perro"))
                .andExpect(jsonPath("$.breed").value("Labrador"));
    }

    @Test
    void addPet_shouldReturn400_whenNameIsBlank() throws Exception {
        request.setName("");

        mockMvc.perform(post("/pets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void addPet_shouldReturn400_whenOwnerIdIsNull() throws Exception {
        request.setOwnerId(null);

        mockMvc.perform(post("/pets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ownerId").exists());
    }

    @Test
    void addPet_shouldReturn400_whenSpeciesIsBlank() throws Exception {
        request.setSpecies("");

        mockMvc.perform(post("/pets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.species").exists());
    }

    @Test
    void addPet_shouldReturn400_whenSexIsNull() throws Exception {
        request.setSex(null);

        mockMvc.perform(post("/pets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sex").exists());
    }
}
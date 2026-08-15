package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetUseCase;
import com.MyAnimaLog.Pets.domain.enums.Sex;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetPetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPetUseCase getPetUseCase;

    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
    }

    @Test
    void getPetById_shouldReturn200_whenPetExists() throws Exception {
        PetResponse response = PetResponse.builder()
                .id(petId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .build();

        when(getPetUseCase.getPetById(any())).thenReturn(response);

        mockMvc.perform(get("/pets/{petId}", petId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Firulais"))
                .andExpect(jsonPath("$.species").value("Perro"));
    }

    @Test
    void getPetById_shouldReturn404_whenPetNotFound() throws Exception {
        when(getPetUseCase.getPetById(any())).thenThrow(new PetNotFoundException());

        mockMvc.perform(get("/pets/{petId}", petId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
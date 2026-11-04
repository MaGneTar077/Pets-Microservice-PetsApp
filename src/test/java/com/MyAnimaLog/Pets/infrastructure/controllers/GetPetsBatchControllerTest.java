package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPetsBatchRequest;
import com.MyAnimaLog.Pets.application.dto.InternalPetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsBatchUseCase;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetPetsBatchController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetPetsBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPetsBatchUseCase getPetsBatchUseCase;

    @Test
    void batch_shouldReturnArrayWithTheExactContractFields() throws Exception {
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(getPetsBatchUseCase.execute(any(GetPetsBatchRequest.class))).thenReturn(List.of(
                new InternalPetResponse(petId, ownerId, "Max", "Perro", "Criollo", Sex.MALE,
                        LocalDate.of(2022, 1, 15), "https://cdn/pets/max.png", null, "NONE", null)));

        mockMvc.perform(post("/internal/pets/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + petId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(petId.toString()))
                .andExpect(jsonPath("$[0].ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$[0].name").value("Max"))
                .andExpect(jsonPath("$[0].species").value("Perro"))
                .andExpect(jsonPath("$[0].breed").value("Criollo"))
                .andExpect(jsonPath("$[0].sex").value("MALE"))
                .andExpect(jsonPath("$[0].birthDate").value("2022-01-15"))
                .andExpect(jsonPath("$[0].photoUrl").value("https://cdn/pets/max.png"))
                .andExpect(jsonPath("$[0].createdByVeterinaryId").isEmpty())
                .andExpect(jsonPath("$[0].claimStatus").value("NONE"))
                .andExpect(jsonPath("$[0].ownerContact").isEmpty());
    }

    @Test
    void batch_shouldReturnEmptyArray_whenNoPetExists() throws Exception {
        when(getPetsBatchUseCase.execute(any(GetPetsBatchRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/internal/pets/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void batch_shouldReturn400_whenMoreThan100Ids() throws Exception {
        when(getPetsBatchUseCase.execute(any(GetPetsBatchRequest.class)))
                .thenThrow(new InvalidPetDataException("No se pueden consultar más de 100 mascotas por petición"));

        mockMvc.perform(post("/internal/pets/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}

package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AgeResponse;
import com.MyAnimaLog.Pets.application.ports.in.CalculateAgeUseCase;
import com.MyAnimaLog.Pets.domain.exceptions.BirthDateNotRegisteredException;
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
        controllers = CalculateAgeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class CalculateAgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalculateAgeUseCase calculateAgeUseCase;

    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
    }

    @Test
    void calculateAge_shouldReturn200_whenPetHasBirthDate() throws Exception {
        AgeResponse response = AgeResponse.builder()
                .years(4)
                .months(2)
                .days(27)
                .build();

        when(calculateAgeUseCase.calculateAge(any())).thenReturn(response);

        mockMvc.perform(get("/pets/{petId}/age", petId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years").value(4))
                .andExpect(jsonPath("$.months").value(2))
                .andExpect(jsonPath("$.days").value(27));
    }

    @Test
    void calculateAge_shouldReturn404_whenPetNotFound() throws Exception {
        when(calculateAgeUseCase.calculateAge(any())).thenThrow(new PetNotFoundException());

        mockMvc.perform(get("/pets/{petId}/age", petId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void calculateAge_shouldReturn422_whenBirthDateNotRegistered() throws Exception {
        when(calculateAgeUseCase.calculateAge(any())).thenThrow(new BirthDateNotRegisteredException());

        mockMvc.perform(get("/pets/{petId}/age", petId)
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }
}
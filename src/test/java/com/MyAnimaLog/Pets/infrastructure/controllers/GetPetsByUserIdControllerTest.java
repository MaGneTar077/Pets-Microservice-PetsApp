package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsByUserIdUseCase;
import com.MyAnimaLog.Pets.domain.enums.Sex;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetPetsByUserIdController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetPetsByUserIdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPetsByUserIdUseCase getPetsByUserIdUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getPetsByUserId_shouldReturn200WithList_whenUserHasPets() throws Exception {
        PetResponse response1 = PetResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(userId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .build();

        PetResponse response2 = PetResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(userId)
                .name("Michi")
                .species("Gato")
                .sex(Sex.FEMALE)
                .build();

        when(getPetsByUserIdUseCase.getPetsByUserId(any())).thenReturn(List.of(response1, response2));

        mockMvc.perform(get("/pets/user/{userId}", userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Firulais"))
                .andExpect(jsonPath("$[1].name").value("Michi"));
    }

    @Test
    void getPetsByUserId_shouldReturn200WithEmptyList_whenUserHasNoPets() throws Exception {
        when(getPetsByUserIdUseCase.getPetsByUserId(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/pets/user/{userId}", userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.PetResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsByUserIdUseCase;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetMyPetsController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetMyPetsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPetsByUserIdUseCase getPetsByUserIdUseCase;

    @Test
    void getMyPets_shouldReturnThePetsOfTheTokenUser() throws Exception {
        UUID petId = UUID.randomUUID();
        when(getPetsByUserIdUseCase.getPetsByUserId(null)).thenReturn(List.of(
                PetResponse.builder()
                        .id(petId)
                        .ownerId(UUID.randomUUID())
                        .name("Firulais")
                        .species("Perro")
                        .sex(Sex.MALE)
                        .build()));

        mockMvc.perform(get("/pets/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(petId.toString()))
                .andExpect(jsonPath("$[0].name").value("Firulais"));
    }

    @Test
    void getMyPets_shouldReturnEmptyArray_whenTheUserHasNoPets() throws Exception {
        when(getPetsByUserIdUseCase.getPetsByUserId(null)).thenReturn(List.of());

        mockMvc.perform(get("/pets/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    /** No debe colisionar con GET /pets/{petId}: "me" es literal y gana. */
    @Test
    void getMyPets_shouldNotBeInterpretedAsAPetId() throws Exception {
        when(getPetsByUserIdUseCase.getPetsByUserId(null)).thenReturn(List.of());

        mockMvc.perform(get("/pets/me"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyPets_shouldReturn403_whenTheIdentityCannotBeResolved() throws Exception {
        when(getPetsByUserIdUseCase.getPetsByUserId(null)).thenThrow(new IdentityMismatchException());

        mockMvc.perform(get("/pets/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IDENTITY_MISMATCH"));
    }
}

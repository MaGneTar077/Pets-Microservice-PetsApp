package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.ports.in.DeletePetUseCase;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotBelongsToOwnerException;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DeletePetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class DeletePetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeletePetUseCase deletePetUseCase;

    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    @Test
    void deletePet_shouldReturn204_whenDeletedSuccessfully() throws Exception {
        doNothing().when(deletePetUseCase).deletePet(any(), any());

        mockMvc.perform(delete("/pets/{petId}", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePet_shouldReturn404_whenPetNotFound() throws Exception {
        doThrow(new PetNotFoundException()).when(deletePetUseCase).deletePet(any(), any());

        mockMvc.perform(delete("/pets/{petId}", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePet_shouldReturn403_whenPetNotBelongsToOwner() throws Exception {
        doThrow(new PetNotBelongsToOwnerException()).when(deletePetUseCase).deletePet(any(), any());

        mockMvc.perform(delete("/pets/{petId}", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
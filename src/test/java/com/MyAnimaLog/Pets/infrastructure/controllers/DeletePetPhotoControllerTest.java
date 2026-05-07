package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.DeletePetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.in.DeletePetPhotoUseCase;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
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

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DeletePetPhotoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class DeletePetPhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeletePetPhotoUseCase deletePetPhotoUseCase;

    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    @Test
    void deletePhoto_shouldReturn200_whenPetHasPhoto() throws Exception {
        DeletePetPhotoResponse response = DeletePetPhotoResponse.builder()
                .petId(petId)
                .message("Photo deleted successfully")
                .build();

        when(deletePetPhotoUseCase.execute(any(DeletePetPhotoRequest.class))).thenReturn(response);

        mockMvc.perform(delete("/pets/{petId}/photo", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.message").value("Photo deleted successfully"));
    }

    @Test
    void deletePhoto_shouldReturn404_whenPetNotFound() throws Exception {
        when(deletePetPhotoUseCase.execute(any(DeletePetPhotoRequest.class)))
                .thenThrow(new NoSuchElementException("Pet not found or does not belong to the owner"));

        mockMvc.perform(delete("/pets/{petId}/photo", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePhoto_shouldReturn400_whenPetHasNoPhoto() throws Exception {
        when(deletePetPhotoUseCase.execute(any(DeletePetPhotoRequest.class)))
                .thenThrow(new InvalidPetDataException("Pet does not have a photo to delete"));

        mockMvc.perform(delete("/pets/{petId}/photo", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
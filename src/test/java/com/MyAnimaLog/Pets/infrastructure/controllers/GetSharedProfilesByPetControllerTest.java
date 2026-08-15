package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesByPetRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesByPetUseCase;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetSharedProfilesByPetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetSharedProfilesByPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSharedProfilesByPetUseCase getSharedProfilesByPetUseCase;

    private UUID petId;
    private UUID ownerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void getSharedProfilesByPet_shouldReturn200WithList_whenRequestIsValid() throws Exception {
        SharedProfileResponse response = SharedProfileResponse.builder()
                .accessId(UUID.randomUUID())
                .petId(petId)
                .userId(userId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();

        when(getSharedProfilesByPetUseCase.execute(any(GetSharedProfilesByPetRequest.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/pets/{petId}/shared", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].petId").value(petId.toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].accessRole").value("EDITOR"));
    }

    @Test
    void getSharedProfilesByPet_shouldReturn200WithEmptyList_whenNoSharedProfiles() throws Exception {
        when(getSharedProfilesByPetUseCase.execute(any(GetSharedProfilesByPetRequest.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/pets/{petId}/shared", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSharedProfilesByPet_shouldReturn404_whenPetNotFound() throws Exception {
        when(getSharedProfilesByPetUseCase.execute(any(GetSharedProfilesByPetRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(get("/pets/{petId}/shared", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getSharedProfilesByPet_shouldReturn403_whenRequesterIsNotOwner() throws Exception {
        when(getSharedProfilesByPetUseCase.execute(any(GetSharedProfilesByPetRequest.class)))
                .thenThrow(new UnauthorizedPetAccessException());

        mockMvc.perform(get("/pets/{petId}/shared", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }
}
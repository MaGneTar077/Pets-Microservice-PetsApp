package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GrantAccessRequest;
import com.MyAnimaLog.Pets.application.dto.GrantAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.GrantAccessUseCase;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.AccessAlreadyGrantedException;
import com.MyAnimaLog.Pets.domain.exceptions.CannotGrantAccessToOwnerException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GrantAccessController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GrantAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GrantAccessUseCase grantAccessUseCase;

    private UUID petId;
    private UUID ownerId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
    }

    private String buildBody(UUID ownerId, UUID targetUserId, String accessRole) throws Exception {
        return objectMapper.writeValueAsString(
                GrantAccessRequest.builder()
                        .ownerId(ownerId)
                        .targetUserId(targetUserId)
                        .accessRole(Rol.valueOf(accessRole))
                        .build()
        );
    }

    @Test
    void grantAccess_shouldReturn201_whenRequestIsValid() throws Exception {
        GrantAccessResponse response = GrantAccessResponse.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .userId(targetUserId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();

        when(grantAccessUseCase.execute(any(GrantAccessRequest.class))).thenReturn(response);

        mockMvc.perform(post("/pets/{petId}/access", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, targetUserId, "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.accessRole").value("EDITOR"));
    }

    @Test
    void grantAccess_shouldReturn404_whenPetNotFound() throws Exception {
        when(grantAccessUseCase.execute(any(GrantAccessRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(post("/pets/{petId}/access", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, targetUserId, "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void grantAccess_shouldReturn403_whenRequesterIsNotOwner() throws Exception {
        when(grantAccessUseCase.execute(any(GrantAccessRequest.class)))
                .thenThrow(new UnauthorizedPetAccessException());

        mockMvc.perform(post("/pets/{petId}/access", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, targetUserId, "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void grantAccess_shouldReturn400_whenTargetIsOwner() throws Exception {
        when(grantAccessUseCase.execute(any(GrantAccessRequest.class)))
                .thenThrow(new CannotGrantAccessToOwnerException());

        mockMvc.perform(post("/pets/{petId}/access", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, ownerId, "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void grantAccess_shouldReturn409_whenAccessAlreadyGranted() throws Exception {
        when(grantAccessUseCase.execute(any(GrantAccessRequest.class)))
                .thenThrow(new AccessAlreadyGrantedException());

        mockMvc.perform(post("/pets/{petId}/access", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, targetUserId, "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
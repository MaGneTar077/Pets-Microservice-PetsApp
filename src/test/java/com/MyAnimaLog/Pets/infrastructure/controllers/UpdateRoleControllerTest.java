package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.UpdateRoleRequest;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleResponse;
import com.MyAnimaLog.Pets.application.ports.in.UpdateRoleUseCase;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UpdateRoleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class UpdateRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdateRoleUseCase updateRoleUseCase;

    private UUID petId;
    private UUID ownerId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
    }

    private String buildBody(UUID ownerId, String newRole) throws Exception {
        return objectMapper.writeValueAsString(
                UpdateRoleRequest.builder()
                        .ownerId(ownerId)
                        .newRole(Rol.valueOf(newRole))
                        .build()
        );
    }

    @Test
    void updateRole_shouldReturn200_whenRequestIsValid() throws Exception {
        UpdateRoleResponse response = UpdateRoleResponse.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .userId(targetUserId)
                .newRole(Rol.VIEWER)
                .updatedAt(LocalDateTime.now())
                .build();

        when(updateRoleUseCase.execute(any(UpdateRoleRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "VIEWER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.newRole").value("VIEWER"));
    }

    @Test
    void updateRole_shouldReturn404_whenPetNotFound() throws Exception {
        when(updateRoleUseCase.execute(any(UpdateRoleRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(patch("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "VIEWER"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateRole_shouldReturn403_whenRequesterIsNotOwner() throws Exception {
        when(updateRoleUseCase.execute(any(UpdateRoleRequest.class)))
                .thenThrow(new UnauthorizedPetAccessException());

        mockMvc.perform(patch("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "VIEWER"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateRole_shouldReturn404_whenAccessNotFound() throws Exception {
        when(updateRoleUseCase.execute(any(UpdateRoleRequest.class)))
                .thenThrow(new AccessNotFoundException());

        mockMvc.perform(patch("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "VIEWER"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
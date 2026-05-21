package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.RevokeAccessRequest;
import com.MyAnimaLog.Pets.application.dto.RevokeAccessResponse;
import com.MyAnimaLog.Pets.application.ports.in.RevokeAccessUseCase;
import com.MyAnimaLog.Pets.domain.exceptions.AccessNotFoundException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RevokeAccessController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class RevokeAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevokeAccessUseCase revokeAccessUseCase;

    private UUID petId;
    private UUID ownerId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
    }

    @Test
    void revokeAccess_shouldReturn200_whenRequestIsValid() throws Exception {
        when(revokeAccessUseCase.execute(any(RevokeAccessRequest.class)))
                .thenReturn(RevokeAccessResponse.builder()
                        .message("Access revoked successfully")
                        .build());

        mockMvc.perform(delete("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access revoked successfully"));
    }

    @Test
    void revokeAccess_shouldReturn404_whenPetNotFound() throws Exception {
        when(revokeAccessUseCase.execute(any(RevokeAccessRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(delete("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void revokeAccess_shouldReturn403_whenRequesterIsNotOwner() throws Exception {
        when(revokeAccessUseCase.execute(any(RevokeAccessRequest.class)))
                .thenThrow(new UnauthorizedPetAccessException());

        mockMvc.perform(delete("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void revokeAccess_shouldReturn404_whenAccessNotFound() throws Exception {
        when(revokeAccessUseCase.execute(any(RevokeAccessRequest.class)))
                .thenThrow(new AccessNotFoundException());

        mockMvc.perform(delete("/pets/{petId}/access/{targetUserId}", petId, targetUserId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
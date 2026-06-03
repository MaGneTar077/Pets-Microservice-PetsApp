package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.ResendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.ResendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.ResendInvitationUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
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
        controllers = ResendInvitationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class ResendInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResendInvitationUseCase resendInvitationUseCase;

    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    private String buildBody(UUID ownerId, String email, String accessRole) throws Exception {
        return objectMapper.writeValueAsString(
                ResendInvitationRequest.builder()
                        .ownerId(ownerId)
                        .email(email)
                        .accessRole(Rol.valueOf(accessRole))
                        .build()
        );
    }

    @Test
    void resendInvitation_shouldReturn200_whenRequestIsValid() throws Exception {
        ResendInvitationResponse response = ResendInvitationResponse.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email("rechazado@email.com")
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(resendInvitationUseCase.execute(any(ResendInvitationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/pets/{petId}/invitations/resend", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "rechazado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.email").value("rechazado@email.com"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void resendInvitation_shouldReturn404_whenPetNotFound() throws Exception {
        when(resendInvitationUseCase.execute(any(ResendInvitationRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(post("/pets/{petId}/invitations/resend", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "rechazado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void resendInvitation_shouldReturn403_whenRequesterIsNotOwner() throws Exception {
        when(resendInvitationUseCase.execute(any(ResendInvitationRequest.class)))
                .thenThrow(new UnauthorizedPetAccessException());

        mockMvc.perform(post("/pets/{petId}/invitations/resend", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "rechazado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void resendInvitation_shouldReturn404_whenInvitationNotFound() throws Exception {
        when(resendInvitationUseCase.execute(any(ResendInvitationRequest.class)))
                .thenThrow(new InvitationNotFoundException());

        mockMvc.perform(post("/pets/{petId}/invitations/resend", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "rechazado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
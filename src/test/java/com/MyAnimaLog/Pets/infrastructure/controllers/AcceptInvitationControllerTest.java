package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.AcceptInvitationUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.AccessAlreadyGrantedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AcceptInvitationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class AcceptInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AcceptInvitationUseCase acceptInvitationUseCase;

    private UUID petId;
    private UUID userId;
    private String token;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();
        token = UUID.randomUUID().toString();
    }

    private String buildBody(UUID userId) throws Exception {
        return objectMapper.writeValueAsString(
                AcceptInvitationRequest.builder()
                        .userId(userId)
                        .build()
        );
    }

    @Test
    void acceptInvitation_shouldReturn200_whenRequestIsValid() throws Exception {
        AcceptInvitationResponse response = AcceptInvitationResponse.builder()
                .invitationId(UUID.randomUUID())
                .petId(petId)
                .userId(userId)
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.ACCEPTED)
                .build();

        when(acceptInvitationUseCase.execute(any(AcceptInvitationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/invitations/{token}/accept", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(userId))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.accessRole").value("EDITOR"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void acceptInvitation_shouldReturn404_whenTokenNotFound() throws Exception {
        when(acceptInvitationUseCase.execute(any(AcceptInvitationRequest.class)))
                .thenThrow(new InvitationNotFoundException());

        mockMvc.perform(post("/invitations/{token}/accept", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(userId))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void acceptInvitation_shouldReturn400_whenInvitationAlreadyProcessed() throws Exception {
        when(acceptInvitationUseCase.execute(any(AcceptInvitationRequest.class)))
                .thenThrow(new InvitationAlreadyProcessedException());

        mockMvc.perform(post("/invitations/{token}/accept", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(userId))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void acceptInvitation_shouldReturn409_whenAccessAlreadyGranted() throws Exception {
        when(acceptInvitationUseCase.execute(any(AcceptInvitationRequest.class)))
                .thenThrow(new AccessAlreadyGrantedException());

        mockMvc.perform(post("/invitations/{token}/accept", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(userId))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
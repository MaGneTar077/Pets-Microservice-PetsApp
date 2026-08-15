package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.SendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.SendInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.SendInvitationUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyExistsException;
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
        controllers = SendInvitationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class SendInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SendInvitationUseCase sendInvitationUseCase;

    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    private String buildBody(UUID ownerId, String email, String accessRole) throws Exception {
        return objectMapper.writeValueAsString(
                SendInvitationRequest.builder()
                        .ownerId(ownerId)
                        .email(email)
                        .accessRole(Rol.valueOf(accessRole))
                        .build()
        );
    }

    @Test
    void sendInvitation_shouldReturn201_whenRequestIsValid() throws Exception {
        SendInvitationResponse response = SendInvitationResponse.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email("invitado@email.com")
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(sendInvitationUseCase.execute(any(SendInvitationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/pets/{petId}/invitations", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "invitado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.email").value("invitado@email.com"))
                .andExpect(jsonPath("$.accessRole").value("EDITOR"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void sendInvitation_shouldReturn404_whenPetNotFound() throws Exception {
        when(sendInvitationUseCase.execute(any(SendInvitationRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(post("/pets/{petId}/invitations", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "invitado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void sendInvitation_shouldReturn403_whenRequesterIsNotOwner() throws Exception {
        when(sendInvitationUseCase.execute(any(SendInvitationRequest.class)))
                .thenThrow(new UnauthorizedPetAccessException());

        mockMvc.perform(post("/pets/{petId}/invitations", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "invitado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void sendInvitation_shouldReturn409_whenInvitationAlreadyExists() throws Exception {
        when(sendInvitationUseCase.execute(any(SendInvitationRequest.class)))
                .thenThrow(new InvitationAlreadyExistsException());

        mockMvc.perform(post("/pets/{petId}/invitations", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(ownerId, "invitado@email.com", "EDITOR"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
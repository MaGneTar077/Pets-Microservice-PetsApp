package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByPetUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
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
        controllers = GetPendingInvitationsByPetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetPendingInvitationsByPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPendingInvitationsByPetUseCase getPendingInvitationsByPetUseCase;

    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    @Test
    void getPendingInvitations_shouldReturn200WithList_whenRequestIsValid() throws Exception {
        PetInvitationResponse response = PetInvitationResponse.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .email("invitado@email.com")
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(getPendingInvitationsByPetUseCase.execute(any(GetPendingInvitationsByPetRequest.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/pets/{petId}/invitations/pending", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].petId").value(petId.toString()))
                .andExpect(jsonPath("$[0].email").value("invitado@email.com"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].accessRole").value("EDITOR"));
    }

    @Test
    void getPendingInvitations_shouldReturn200WithEmptyList_whenNoPendingInvitations() throws Exception {
        when(getPendingInvitationsByPetUseCase.execute(any(GetPendingInvitationsByPetRequest.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/pets/{petId}/invitations/pending", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPendingInvitations_shouldReturn404_whenPetNotFound() throws Exception {
        when(getPendingInvitationsByPetUseCase.execute(any(GetPendingInvitationsByPetRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(get("/pets/{petId}/invitations/pending", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getPendingInvitations_shouldReturn403_whenRequesterIsNotOwner() throws Exception {
        when(getPendingInvitationsByPetUseCase.execute(any(GetPendingInvitationsByPetRequest.class)))
                .thenThrow(new UnauthorizedPetAccessException());

        mockMvc.perform(get("/pets/{petId}/invitations/pending", petId)
                        .param("ownerId", ownerId.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }
}
package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByEmailUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetPendingInvitationsByEmailController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetPendingInvitationsByEmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetPendingInvitationsByEmailUseCase getPendingInvitationsByEmailUseCase;

    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
    }

    private String buildBody(String email) throws Exception {
        return objectMapper.writeValueAsString(
                GetPendingInvitationsByEmailRequest.builder()
                        .email(email)
                        .build()
        );
    }

    @Test
    void getPendingInvitationsByEmail_shouldReturn200WithList_whenInvitationsExist() throws Exception {
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

        when(getPendingInvitationsByEmailUseCase.execute(any(GetPendingInvitationsByEmailRequest.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/pets/invitations/pending")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody("invitado@email.com"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("invitado@email.com"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].accessRole").value("EDITOR"));
    }

    @Test
    void getPendingInvitationsByEmail_shouldReturn200WithEmptyList_whenNoInvitationsExist() throws Exception {
        when(getPendingInvitationsByEmailUseCase.execute(any(GetPendingInvitationsByEmailRequest.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/pets/invitations/pending")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody("invitado@email.com"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
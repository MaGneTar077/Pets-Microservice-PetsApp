package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.RejectInvitationUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationAlreadyProcessedException;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RejectInvitationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class RejectInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RejectInvitationUseCase rejectInvitationUseCase;

    private UUID petId;
    private String token;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        token = UUID.randomUUID().toString();
    }

    @Test
    void rejectInvitation_shouldReturn200_whenRequestIsValid() throws Exception {
        RejectInvitationResponse response = RejectInvitationResponse.builder()
                .invitationId(UUID.randomUUID())
                .petId(petId)
                .status(InvitationStatus.REJECTED)
                .build();

        when(rejectInvitationUseCase.execute(any())).thenReturn(response);

        mockMvc.perform(post("/invitations/{token}/reject", token)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectInvitation_shouldReturn404_whenTokenNotFound() throws Exception {
        when(rejectInvitationUseCase.execute(any()))
                .thenThrow(new InvitationNotFoundException());

        mockMvc.perform(post("/invitations/{token}/reject", token)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void rejectInvitation_shouldReturn400_whenInvitationAlreadyProcessed() throws Exception {
        when(rejectInvitationUseCase.execute(any()))
                .thenThrow(new InvitationAlreadyProcessedException());

        mockMvc.perform(post("/invitations/{token}/reject", token)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
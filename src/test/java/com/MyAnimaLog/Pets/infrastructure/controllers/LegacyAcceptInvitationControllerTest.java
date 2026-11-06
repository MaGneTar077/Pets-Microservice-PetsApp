package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.AcceptInvitationUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
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

/** Alias @Deprecated: POST /invitations/{token}/accept, ruta previa a la 686c5e5. */
@WebMvcTest(
        controllers = LegacyAcceptInvitationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class LegacyAcceptInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcceptInvitationUseCase acceptInvitationUseCase;

    @Test
    void acceptInvitation_shouldReturn200_onTheLegacyRoute() throws Exception {
        String token = UUID.randomUUID().toString();
        AcceptInvitationResponse response = AcceptInvitationResponse.builder()
                .invitationId(UUID.randomUUID())
                .petId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .accessRole(Rol.EDITOR)
                .status(InvitationStatus.ACCEPTED)
                .build();

        when(acceptInvitationUseCase.execute(any(AcceptInvitationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/invitations/{token}/accept", token).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void acceptInvitation_shouldApplyTheSameRules_onTheLegacyRoute() throws Exception {
        String token = UUID.randomUUID().toString();
        when(acceptInvitationUseCase.execute(any(AcceptInvitationRequest.class)))
                .thenThrow(new InvitationEmailMismatchException());

        mockMvc.perform(post("/invitations/{token}/accept", token).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVITATION_EMAIL_MISMATCH"));
    }
}

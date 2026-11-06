package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPendingInvitationsByEmailUseCase;
import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import com.MyAnimaLog.Pets.domain.enums.Rol;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetMyPendingInvitationsController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetMyPendingInvitationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPendingInvitationsByEmailUseCase getPendingInvitationsByEmailUseCase;

    @Test
    void getMyPendingInvitations_shouldReturnThePendingInvitationsOfTheTokenUser() throws Exception {
        UUID petId = UUID.randomUUID();

        when(getPendingInvitationsByEmailUseCase.execute(any(GetPendingInvitationsByEmailRequest.class)))
                .thenReturn(List.of(PetInvitationResponse.builder()
                        .id(UUID.randomUUID())
                        .petId(petId)
                        .email("invitado@email.com")
                        .accessRole(Rol.EDITOR)
                        .status(InvitationStatus.PENDING)
                        .token(UUID.randomUUID().toString())
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .createdAt(LocalDateTime.now())
                        .build()));

        mockMvc.perform(get("/pets/invitations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].petId").value(petId.toString()))
                .andExpect(jsonPath("$[0].email").value("invitado@email.com"));
    }

    @Test
    void getMyPendingInvitations_shouldReturnEmptyArray_whenNothingIsPending() throws Exception {
        when(getPendingInvitationsByEmailUseCase.execute(any(GetPendingInvitationsByEmailRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/pets/invitations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}

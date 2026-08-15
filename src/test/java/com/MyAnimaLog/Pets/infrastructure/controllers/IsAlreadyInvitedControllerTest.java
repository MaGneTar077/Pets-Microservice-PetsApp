package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedRequest;
import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsAlreadyInvitedUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = IsAlreadyInvitedController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class IsAlreadyInvitedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IsAlreadyInvitedUseCase isAlreadyInvitedUseCase;

    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
    }

    @Test
    void isAlreadyInvited_shouldReturn200WithTrue_whenPendingInvitationExists() throws Exception {
        when(isAlreadyInvitedUseCase.execute(any(IsAlreadyInvitedRequest.class)))
                .thenReturn(IsAlreadyInvitedResponse.builder().invited(true).build());

        mockMvc.perform(get("/pets/{petId}/invitations/check", petId)
                        .param("email", "invitado@email.com")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invited").value(true));
    }

    @Test
    void isAlreadyInvited_shouldReturn200WithFalse_whenNoPendingInvitationExists() throws Exception {
        when(isAlreadyInvitedUseCase.execute(any(IsAlreadyInvitedRequest.class)))
                .thenReturn(IsAlreadyInvitedResponse.builder().invited(false).build());

        mockMvc.perform(get("/pets/{petId}/invitations/check", petId)
                        .param("email", "invitado@email.com")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invited").value(false));
    }
}
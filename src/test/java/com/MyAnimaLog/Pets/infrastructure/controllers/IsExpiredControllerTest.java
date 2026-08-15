package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.IsExpiredRequest;
import com.MyAnimaLog.Pets.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsExpiredUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = IsExpiredController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class IsExpiredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IsExpiredUseCase isExpiredUseCase;

    private String token;

    @BeforeEach
    void setUp() {
        token = UUID.randomUUID().toString();
    }

    @Test
    void isExpired_shouldReturn200WithFalse_whenInvitationIsNotExpired() throws Exception {
        when(isExpiredUseCase.execute(any(IsExpiredRequest.class)))
                .thenReturn(IsExpiredResponse.builder().expired(false).build());

        mockMvc.perform(get("/pets/invitations/{token}/expired", token)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(false));
    }

    @Test
    void isExpired_shouldReturn200WithTrue_whenInvitationIsExpired() throws Exception {
        when(isExpiredUseCase.execute(any(IsExpiredRequest.class)))
                .thenReturn(IsExpiredResponse.builder().expired(true).build());

        mockMvc.perform(get("/pets/invitations/{token}/expired", token)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(true));
    }

    @Test
    void isExpired_shouldReturn404_whenTokenNotFound() throws Exception {
        when(isExpiredUseCase.execute(any(IsExpiredRequest.class)))
                .thenThrow(new InvitationNotFoundException());

        mockMvc.perform(get("/pets/invitations/{token}/expired", token)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.HasPermissionRequest;
import com.MyAnimaLog.Pets.application.dto.HasPermissionResponse;
import com.MyAnimaLog.Pets.application.ports.in.HasPermissionUseCase;
import com.MyAnimaLog.Pets.domain.enums.Rol;
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
        controllers = HasPermissionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class HasPermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HasPermissionUseCase hasPermissionUseCase;

    private UUID petId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void hasPermission_shouldReturn200WithTrueAndRole_whenAccessExists() throws Exception {
        HasPermissionResponse response = HasPermissionResponse.builder()
                .hasPermission(true)
                .role(Rol.EDITOR)
                .build();

        when(hasPermissionUseCase.execute(any(HasPermissionRequest.class))).thenReturn(response);

        mockMvc.perform(get("/pets/{petId}/access/{userId}", petId, userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPermission").value(true))
                .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    @Test
    void hasPermission_shouldReturn200WithFalseAndNullRole_whenAccessDoesNotExist() throws Exception {
        HasPermissionResponse response = HasPermissionResponse.builder()
                .hasPermission(false)
                .role(null)
                .build();

        when(hasPermissionUseCase.execute(any(HasPermissionRequest.class))).thenReturn(response);

        mockMvc.perform(get("/pets/{petId}/access/{userId}", petId, userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPermission").value(false))
                .andExpect(jsonPath("$.role").doesNotExist());
    }
}
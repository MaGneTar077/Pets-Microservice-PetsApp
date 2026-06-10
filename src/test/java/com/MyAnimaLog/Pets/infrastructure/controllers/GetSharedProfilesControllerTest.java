package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetSharedProfilesUseCase;
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
        controllers = GetSharedProfilesController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetSharedProfilesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSharedProfilesUseCase getSharedProfilesUseCase;

    private UUID userId;
    private UUID petId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        petId = UUID.randomUUID();
    }

    @Test
    void getSharedProfiles_shouldReturn200WithList_whenProfilesExist() throws Exception {
        SharedProfileResponse response = SharedProfileResponse.builder()
                .accessId(UUID.randomUUID())
                .petId(petId)
                .userId(userId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();

        when(getSharedProfilesUseCase.execute(any(GetSharedProfilesRequest.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/pets/shared")
                        .param("userId", userId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].petId").value(petId.toString()))
                .andExpect(jsonPath("$[0].accessRole").value("EDITOR"));
    }

    @Test
    void getSharedProfiles_shouldReturn200WithEmptyList_whenNoProfilesExist() throws Exception {
        when(getSharedProfilesUseCase.execute(any(GetSharedProfilesRequest.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/pets/shared")
                        .param("userId", userId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
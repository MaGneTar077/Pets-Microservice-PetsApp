package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;
import com.MyAnimaLog.Pets.application.ports.in.IsOwnerUseCase;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
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
        controllers = IsOwnerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class IsOwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IsOwnerUseCase isOwnerUseCase;

    private UUID petId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void isOwner_shouldReturn200WithTrue_whenUserIsOwner() throws Exception {
        when(isOwnerUseCase.execute(any(IsOwnerRequest.class)))
                .thenReturn(IsOwnerResponse.builder().isOwner(true).build());

        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(true));
    }

    @Test
    void isOwner_shouldReturn200WithFalse_whenUserIsNotOwner() throws Exception {
        when(isOwnerUseCase.execute(any(IsOwnerRequest.class)))
                .thenReturn(IsOwnerResponse.builder().isOwner(false).build());

        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(false));
    }

    @Test
    void isOwner_shouldReturn404_whenPetNotFound() throws Exception {
        when(isOwnerUseCase.execute(any(IsOwnerRequest.class)))
                .thenThrow(new PetNotFoundException(petId));

        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, userId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
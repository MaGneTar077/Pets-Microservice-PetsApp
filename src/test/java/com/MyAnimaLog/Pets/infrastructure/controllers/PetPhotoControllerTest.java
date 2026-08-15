package com.MyAnimaLog.Pets.infrastructure.controllers;

import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoRequest;
import com.MyAnimaLog.Pets.application.dto.UploadPetPhotoResponse;
import com.MyAnimaLog.Pets.application.ports.in.UploadPetPhotoUseCase;
import com.MyAnimaLog.Pets.domain.exceptions.FileSizeExceededException;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidImageFormatException;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PetPhotoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class PetPhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadPetPhotoUseCase uploadPetPhotoUseCase;

    private UUID petId;
    private UUID ownerId;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        validFile = new MockMultipartFile(
                "file",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1024]
        );
    }

    @Test
    void uploadPhoto_shouldReturn200_whenRequestIsValid() throws Exception {
        UploadPetPhotoResponse response = UploadPetPhotoResponse.builder()
                .petId(petId)
                .photoUrl("https://supabase.co/storage/v1/object/public/pet-photos/pets/" + petId + "/profile.jpg")
                .message("Photo uploaded successfully")
                .build();

        when(uploadPetPhotoUseCase.execute(any(UploadPetPhotoRequest.class))).thenReturn(response);

        mockMvc.perform(multipart("/pets/{petId}/photo", petId)
                        .file(validFile)
                        .param("ownerId", ownerId.toString())
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.message").value("Photo uploaded successfully"))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());
    }

    @Test
    void uploadPhoto_shouldReturn404_whenPetNotFound() throws Exception {
        when(uploadPetPhotoUseCase.execute(any(UploadPetPhotoRequest.class)))
                .thenThrow(new NoSuchElementException("Pet not found or does not belong to the owner"));

        mockMvc.perform(multipart("/pets/{petId}/photo", petId)
                        .file(validFile)
                        .param("ownerId", ownerId.toString())
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadPhoto_shouldReturn415_whenImageFormatIsInvalid() throws Exception {
        when(uploadPetPhotoUseCase.execute(any(UploadPetPhotoRequest.class)))
                .thenThrow(new InvalidImageFormatException("Invalid image format"));

        mockMvc.perform(multipart("/pets/{petId}/photo", petId)
                        .file(validFile)
                        .param("ownerId", ownerId.toString())
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(csrf()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void uploadPhoto_shouldReturn413_whenFileSizeExceeded() throws Exception {
        when(uploadPetPhotoUseCase.execute(any(UploadPetPhotoRequest.class)))
                .thenThrow(new FileSizeExceededException("File size exceeds the 5 MB limit"));

        mockMvc.perform(multipart("/pets/{petId}/photo", petId)
                        .file(validFile)
                        .param("ownerId", ownerId.toString())
                        .with(request -> { request.setMethod("PATCH"); return request; })
                        .with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }
}
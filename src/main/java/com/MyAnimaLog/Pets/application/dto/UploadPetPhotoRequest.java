package com.MyAnimaLog.Pets.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadPetPhotoRequest {
    private UUID petId;
    private UUID ownerId;
    private MultipartFile file;
}

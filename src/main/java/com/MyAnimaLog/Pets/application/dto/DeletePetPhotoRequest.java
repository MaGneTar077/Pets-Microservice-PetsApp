package com.MyAnimaLog.Pets.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeletePetPhotoRequest {
    private UUID petId;
    private UUID ownerId;
}

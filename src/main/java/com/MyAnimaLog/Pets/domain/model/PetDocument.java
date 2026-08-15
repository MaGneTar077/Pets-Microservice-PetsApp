package com.MyAnimaLog.Pets.domain.model;

import com.MyAnimaLog.Pets.domain.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class PetDocument {
    private UUID id;
    private UUID petId;
    private UUID uploadedBy;
    private String title;
    private String description;
    private String fileUrl;
    private String mimeType;
    private Long fileSizeBytes;
    private DocumentType documentType;
    private LocalDateTime uploadedAt;
    private Boolean active;
}

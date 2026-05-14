package com.MyAnimaLog.Pets.application.dto;

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
@Builder
public class PetDocumentSummaryResponse {
    private UUID id;
    private String title;
    private String description;
    private String fileUrl;
    private String mimeType;
    private Long fileSizeBytes;
    private DocumentType documentType;
    private LocalDateTime uploadedAt;
}

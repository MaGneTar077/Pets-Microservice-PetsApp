package com.MyAnimaLog.Pets.infrastructure.mapper;

import com.MyAnimaLog.Pets.domain.model.PetDocument;
import com.MyAnimaLog.Pets.infrastructure.entity.PetDocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class PetDocumentMapper {

    public PetDocumentEntity toEntity(PetDocument document) {
        return PetDocumentEntity.builder()
                .id(document.getId())
                .petId(document.getPetId())
                .uploadedBy(document.getUploadedBy())
                .title(document.getTitle())
                .description(document.getDescription())
                .fileUrl(document.getFileUrl())
                .mimeType(document.getMimeType())
                .fileSizeBytes(document.getFileSizeBytes())
                .documentType(document.getDocumentType())
                .uploadedAt(document.getUploadedAt())
                .active(document.getActive())
                .build();
    }

    public PetDocument toDomain(PetDocumentEntity entity) {
        return PetDocument.builder()
                .id(entity.getId())
                .petId(entity.getPetId())
                .uploadedBy(entity.getUploadedBy())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .fileUrl(entity.getFileUrl())
                .mimeType(entity.getMimeType())
                .fileSizeBytes(entity.getFileSizeBytes())
                .documentType(entity.getDocumentType())
                .uploadedAt(entity.getUploadedAt())
                .active(entity.getActive())
                .build();
    }
}

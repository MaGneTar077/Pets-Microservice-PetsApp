package com.MyAnimaLog.Pets.application.ports.out;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PetDocumentStoragePort {
    String upload(UUID petId, UUID documentId, MultipartFile file);
    void delete(UUID petId, String fileUrl);
    String getDownloadUrl(String fileUrl);
}

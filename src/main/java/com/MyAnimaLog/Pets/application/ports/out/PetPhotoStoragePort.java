package com.MyAnimaLog.Pets.application.ports.out;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PetPhotoStoragePort {
    String upload(UUID petId, MultipartFile file);
    void delete(UUID petId, String photoUrl);
}

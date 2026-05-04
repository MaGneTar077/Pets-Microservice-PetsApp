package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.ports.out.PetPhotoStoragePort;
import com.MyAnimaLog.Pets.domain.exceptions.FileUploadException;
import com.MyAnimaLog.Pets.infrastructure.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupabasePetPhotoStorageAdapter implements PetPhotoStoragePort {

    private static final String BUCKET = "pet-photos";

    private final RestTemplate restTemplate;
    private final SupabaseConfig supabaseConfig;

    @Override
    public String upload(UUID petId, MultipartFile file) {
        try {
            String fileName = buildFileName(petId, file.getOriginalFilename());
            String uploadUrl = buildUploadUrl(fileName);

            HttpHeaders headers = buildHeaders(file.getContentType());
            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new FileUploadException("Supabase rejected the upload: " + response.getBody());
            }

            return buildPublicUrl(fileName);

        } catch (FileUploadException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error uploading photo for pet {}: {}", petId, ex.getMessage());
            throw new FileUploadException("Failed to upload photo", ex);
        }
    }

    @Override
    public void delete(UUID petId, String photoUrl) {
        try {
            String fileName = extractFileNameFromUrl(photoUrl);
            String deleteUrl = buildUploadUrl(fileName);

            HttpHeaders headers = buildHeaders(null);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);

        } catch (Exception ex) {
            log.warn("Could not delete photo from storage for pet {}: {}", petId, ex.getMessage());
        }
    }

    private String buildFileName(UUID petId, String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return "pets/" + petId + "/profile" + extension;
    }

    private String buildUploadUrl(String fileName) {
        return supabaseConfig.getSupabaseUrl()
                + "/storage/v1/object/"
                + BUCKET + "/"
                + fileName
                + "?upsert=true";
    }

    private String buildPublicUrl(String fileName) {
        return supabaseConfig.getSupabaseUrl()
                + "/storage/v1/object/public/"
                + BUCKET + "/"
                + fileName;
    }

    private String extractFileNameFromUrl(String photoUrl) {
        String marker = "/object/public/" + BUCKET + "/";
        int idx = photoUrl.indexOf(marker);
        if (idx == -1) {
            throw new FileUploadException("Cannot extract file name from URL: " + photoUrl);
        }
        return photoUrl.substring(idx + marker.length());
    }

    private HttpHeaders buildHeaders(String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseConfig.getSupabaseKey());
        headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
        if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }
        return headers;
    }
}
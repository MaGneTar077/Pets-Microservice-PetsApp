package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.ports.out.PetDocumentStoragePort;
import com.MyAnimaLog.Pets.domain.exceptions.DocumentUploadException;
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
public class SupabasePetDocumentStorageAdapter implements PetDocumentStoragePort {

    private static final String BUCKET = "pet-documents";

    private final RestTemplate restTemplate;
    private final SupabaseConfig supabaseConfig;

    @Override
    public String upload(UUID petId, UUID documentId, MultipartFile file) {
        try {
            String fileName = buildFileName(petId, documentId, file.getOriginalFilename());
            String uploadUrl = buildUploadUrl(fileName);

            HttpHeaders headers = buildHeaders(file.getContentType());
            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DocumentUploadException("Supabase rejected the upload: " + response.getBody());
            }

            return buildPublicUrl(fileName);

        } catch (DocumentUploadException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error uploading document for pet {}: {}", petId, ex.getMessage());
            throw new DocumentUploadException("Failed to upload document", ex);
        }
    }

    @Override
    public void delete(UUID petId, String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            String deleteUrl = supabaseConfig.getSupabaseUrl()
                    + "/storage/v1/object/"
                    + BUCKET;

            HttpHeaders headers = buildHeaders("application/json");
            String body = "{\"prefixes\": [\"" + fileName + "\"]}";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl, HttpMethod.DELETE, entity, String.class);

            log.info("Document deleted from storage: {}", fileName);

        } catch (Exception ex) {
            log.warn("Could not delete document from storage for pet {}: {}", petId, ex.getMessage());
        }
    }

    private String buildFileName(UUID petId, UUID documentId, String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return "pets/" + petId + "/documents/" + documentId + extension;
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

    private String extractFileNameFromUrl(String fileUrl) {
        String marker = "/object/public/" + BUCKET + "/";
        int idx = fileUrl.indexOf(marker);
        if (idx == -1) {
            throw new DocumentUploadException("Cannot extract file name from URL: " + fileUrl);
        }
        return fileUrl.substring(idx + marker.length());
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

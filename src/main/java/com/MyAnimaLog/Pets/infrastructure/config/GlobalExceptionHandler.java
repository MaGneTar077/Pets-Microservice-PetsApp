package com.MyAnimaLog.Pets.infrastructure.config;

import com.MyAnimaLog.Pets.domain.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PetNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePetNotFound(PetNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PetNotBelongsToOwnerException.class)
    public ResponseEntity<Map<String, String>> handleNotBelongs(PetNotBelongsToOwnerException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidPetDataException.class)
    public ResponseEntity<Map<String, String>> handleInvalidData(InvalidPetDataException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Error inesperado: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    @ExceptionHandler(BirthDateNotRegisteredException.class)
    public ResponseEntity<Map<String, String>> handleBirthDateNotRegistered(BirthDateNotRegisteredException ex) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> buildError(HttpStatus status, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(InvalidImageFormatException.class)
    public ResponseEntity<Map<String, String>> handleInvalidImage(InvalidImageFormatException ex) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleFileSizeExceeded(FileSizeExceededException ex) {
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<Map<String, String>> handleFileUpload(FileUploadException ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DocumentUploadException.class)
    public ResponseEntity<Map<String, String>> handleDocumentUpload(DocumentUploadException ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDocumentNotFound(DocumentNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFile(InvalidFileException ex) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(DocumentNotBelongsToPetException.class)
    public ResponseEntity<Map<String, String>> handleDocumentNotBelongs(DocumentNotBelongsToPetException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}

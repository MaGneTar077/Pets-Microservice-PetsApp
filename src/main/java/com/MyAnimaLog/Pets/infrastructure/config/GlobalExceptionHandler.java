package com.MyAnimaLog.Pets.infrastructure.config;

import com.MyAnimaLog.Pets.domain.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "NOT_PET_OWNER");
    }

    @ExceptionHandler(IdentityMismatchException.class)
    public ResponseEntity<Map<String, String>> handleIdentityMismatch(IdentityMismatchException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "IDENTITY_MISMATCH");
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

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Map<String, String>> handleRouteNotFound(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND, "Ruta no encontrada");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido para esta ruta");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + ex.getName());
    }

    /** El detalle solo va al log: la respuesta nunca expone SQL ni stack traces. */
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

    /**
     * Igual que {@link #buildError(HttpStatus, String)} pero añadiendo {@code code}, para que el
     * frontend distinga el motivo sin parsear el mensaje. Solo lo llevan las excepciones que tienen
     * un código definido; el campo {@code error} no cambia.
     */
    private ResponseEntity<Map<String, String>> buildError(HttpStatus status, String message, String code) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("code", code);
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
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "DOCUMENT_NOT_IN_PET");
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidDocument(InvalidDocumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedPetAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedPetAccess(UnauthorizedPetAccessException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "PET_ACCESS_DENIED");
    }

    @ExceptionHandler(CannotGrantAccessToOwnerException.class)
    public ResponseEntity<Map<String, String>> handleCannotGrantAccessToOwner(CannotGrantAccessToOwnerException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "CANNOT_SHARE_WITH_OWNER");
    }

    @ExceptionHandler(AccessAlreadyGrantedException.class)
    public ResponseEntity<Map<String, String>> handleAccessAlreadyGranted(AccessAlreadyGrantedException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), "ACCESS_ALREADY_GRANTED");
    }

    @ExceptionHandler(AccessNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAccessNotFound(AccessNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleInvitationNotFound(InvitationNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvitationAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleInvitationAlreadyExists(InvitationAlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvitationAlreadyProcessedException.class)
    public ResponseEntity<Map<String, String>> handleInvitationAlreadyProcessed(InvitationAlreadyProcessedException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthenticated(UnauthenticatedException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}

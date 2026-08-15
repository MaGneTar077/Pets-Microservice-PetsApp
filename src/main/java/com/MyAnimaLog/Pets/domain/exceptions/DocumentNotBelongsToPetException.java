package com.MyAnimaLog.Pets.domain.exceptions;

import java.util.UUID;

public class DocumentNotBelongsToPetException extends RuntimeException {

    public DocumentNotBelongsToPetException() {
        super("Document does not belong to this pet");
    }

    public DocumentNotBelongsToPetException(String message) {
        super(message);
    }

    public DocumentNotBelongsToPetException(UUID documentId, UUID petId) {
        super("Document " + documentId + " does not belong to pet " + petId);
    }
}

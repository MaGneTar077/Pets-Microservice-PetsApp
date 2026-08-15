package com.MyAnimaLog.Pets.domain.exceptions;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException() {
        super("Document not found");
    }

    public DocumentNotFoundException(String message) {
        super(message);
    }

    public DocumentNotFoundException(UUID documentId) {
        super("Document not found with id: " + documentId);
    }
}

package com.MyAnimaLog.Pets.domain.exceptions;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException() {
        super("Document not found");
    }

    public DocumentNotFoundException(String message) {
        super(message);
    }
}

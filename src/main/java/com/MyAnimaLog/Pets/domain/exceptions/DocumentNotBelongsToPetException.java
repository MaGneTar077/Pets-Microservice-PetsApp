package com.MyAnimaLog.Pets.domain.exceptions;

public class DocumentNotBelongsToPetException extends RuntimeException {

    public DocumentNotBelongsToPetException() {
        super("Document does not belong to this pet");
    }

    public DocumentNotBelongsToPetException(String message) {
        super(message);
    }
}

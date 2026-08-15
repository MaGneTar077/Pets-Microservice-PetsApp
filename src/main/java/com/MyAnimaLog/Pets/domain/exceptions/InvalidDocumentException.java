package com.MyAnimaLog.Pets.domain.exceptions;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(String message) {
        super(message);
    }
}

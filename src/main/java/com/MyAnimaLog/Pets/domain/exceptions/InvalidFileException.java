package com.MyAnimaLog.Pets.domain.exceptions;

public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}

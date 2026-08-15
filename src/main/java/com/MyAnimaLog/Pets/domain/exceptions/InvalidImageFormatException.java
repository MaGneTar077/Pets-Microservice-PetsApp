package com.MyAnimaLog.Pets.domain.exceptions;

public class InvalidImageFormatException extends RuntimeException {
    public InvalidImageFormatException(String message) {
        super(message);
    }
}

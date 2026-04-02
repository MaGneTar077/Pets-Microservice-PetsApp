package com.MyAnimaLog.Pets.domain.exceptions;

public class InvalidPetDataException extends RuntimeException {
    public InvalidPetDataException(String message) {
        super(message);
    }
}

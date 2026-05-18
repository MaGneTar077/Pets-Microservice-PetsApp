package com.MyAnimaLog.Pets.domain.exceptions;

public class AccessAlreadyGrantedException extends RuntimeException {

    public AccessAlreadyGrantedException() {
        super("User already has access to this pet");
    }

    public AccessAlreadyGrantedException(String message) {
        super(message);
    }
}

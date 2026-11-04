package com.MyAnimaLog.Pets.domain.exceptions;

public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("Authentication required");
    }

    public UnauthenticatedException(String message) {
        super(message);
    }
}

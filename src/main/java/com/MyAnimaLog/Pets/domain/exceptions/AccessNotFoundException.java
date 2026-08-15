package com.MyAnimaLog.Pets.domain.exceptions;

public class AccessNotFoundException extends RuntimeException {

    public AccessNotFoundException() {
        super("Access not found for this user and pet");
    }

    public AccessNotFoundException(String message) {
        super(message);
    }
}

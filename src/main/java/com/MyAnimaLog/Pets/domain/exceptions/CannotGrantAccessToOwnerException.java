package com.MyAnimaLog.Pets.domain.exceptions;

public class CannotGrantAccessToOwnerException extends RuntimeException {

    public CannotGrantAccessToOwnerException() {
        super("Cannot grant access to the pet owner");
    }

    public CannotGrantAccessToOwnerException(String message) {
        super(message);
    }
}

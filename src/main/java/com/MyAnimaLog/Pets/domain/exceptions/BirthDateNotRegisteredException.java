package com.MyAnimaLog.Pets.domain.exceptions;

public class BirthDateNotRegisteredException extends RuntimeException {

    public BirthDateNotRegisteredException() {
        super("The pet has no registered date of birth");
    }

    public BirthDateNotRegisteredException(String message) {
        super(message);
    }
}

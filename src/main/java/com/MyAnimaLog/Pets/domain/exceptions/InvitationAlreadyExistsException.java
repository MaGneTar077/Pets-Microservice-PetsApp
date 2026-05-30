package com.MyAnimaLog.Pets.domain.exceptions;

public class InvitationAlreadyExistsException extends RuntimeException {

    public InvitationAlreadyExistsException() {
        super("A pending invitation already exists for this user and pet");
    }

    public InvitationAlreadyExistsException(String message) {
        super(message);
    }
}

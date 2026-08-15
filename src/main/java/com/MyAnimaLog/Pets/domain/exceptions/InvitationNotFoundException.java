package com.MyAnimaLog.Pets.domain.exceptions;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("Invitation not found");
    }

    public InvitationNotFoundException(String message) {
        super(message);
    }
}

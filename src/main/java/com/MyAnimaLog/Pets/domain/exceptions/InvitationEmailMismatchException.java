package com.MyAnimaLog.Pets.domain.exceptions;

/** El email del token no coincide con el de la invitación (sin distinguir mayúsculas). */
public class InvitationEmailMismatchException extends RuntimeException {

    public InvitationEmailMismatchException() {
        super("The authenticated user's email does not match the invitation's email");
    }
}

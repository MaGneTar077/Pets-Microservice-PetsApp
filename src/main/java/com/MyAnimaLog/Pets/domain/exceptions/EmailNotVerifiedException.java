package com.MyAnimaLog.Pets.domain.exceptions;

/** El token tiene {@code email_verified = false}: no puede aceptar ni rechazar invitaciones. */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("The authenticated user's email is not verified");
    }
}

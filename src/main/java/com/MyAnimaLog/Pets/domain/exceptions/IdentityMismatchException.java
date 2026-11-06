package com.MyAnimaLog.Pets.domain.exceptions;

/**
 * El cliente envió un identificador (ownerId, userId, uploadedBy, email) distinto al del token.
 * Mientras el frontend migra se acepta que lo siga mandando, pero solo si coincide con el token.
 */
public class IdentityMismatchException extends RuntimeException {

    public IdentityMismatchException() {
        super("The identity sent in the request does not match the authenticated user");
    }

    public IdentityMismatchException(String message) {
        super(message);
    }
}

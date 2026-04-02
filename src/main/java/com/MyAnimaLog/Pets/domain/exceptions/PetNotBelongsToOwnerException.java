package com.MyAnimaLog.Pets.domain.exceptions;

public class PetNotBelongsToOwnerException extends RuntimeException {
    public PetNotBelongsToOwnerException() {
        super("No tienes permiso para realizar esta acción sobre esta mascota");
    }

        public PetNotBelongsToOwnerException(String message) {
            super(message);
        }
}

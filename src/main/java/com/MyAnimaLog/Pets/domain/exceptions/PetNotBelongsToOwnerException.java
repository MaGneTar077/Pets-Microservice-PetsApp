package com.MyAnimaLog.Pets.domain.exceptions;

public class PetNotBelongsToOwnerException extends RuntimeException {
    public PetNotBelongsToOwnerException() {
        super("You do not have permission to perform this action on this pet.");
    }

        public PetNotBelongsToOwnerException(String message) {
            super(message);
        }
}

package com.MyAnimaLog.Pets.domain.exceptions;

public class PetNotFoundException extends RuntimeException {
    public PetNotFoundException(String id) {
        super("Pet not found with id: " + id);
    }

        public PetNotFoundException() {
            super("Pet not found");
        }
}

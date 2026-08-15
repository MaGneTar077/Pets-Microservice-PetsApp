package com.MyAnimaLog.Pets.domain.exceptions;

import java.util.UUID;

public class PetNotFoundException extends RuntimeException {
    public PetNotFoundException(String id) {
        super("Pet not found with id: " + id);
    }

    public PetNotFoundException(UUID petId) {
        super("Pet not found with id: " + petId);
    }

        public PetNotFoundException() {
            super("Pet not found");
        }
}

package com.MyAnimaLog.Pets.domain.exceptions;

public class PetNotFoundException extends RuntimeException {
    public PetNotFoundException(String id) {
        super("Mascota no encontrada con id: " + id);
    }

        public PetNotFoundException() {
            super("Mascota no encontrada");
        }
}

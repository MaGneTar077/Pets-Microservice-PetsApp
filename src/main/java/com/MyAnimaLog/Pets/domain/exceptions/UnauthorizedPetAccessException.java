package com.MyAnimaLog.Pets.domain.exceptions;

public class UnauthorizedPetAccessException extends RuntimeException {

  public UnauthorizedPetAccessException() {
    super("You do not have permission to perform this action");
  }

    public UnauthorizedPetAccessException(String message) {
        super(message);
    }
}

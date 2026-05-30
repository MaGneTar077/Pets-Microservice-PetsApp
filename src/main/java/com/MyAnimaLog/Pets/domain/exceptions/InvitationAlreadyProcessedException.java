package com.MyAnimaLog.Pets.domain.exceptions;

public class InvitationAlreadyProcessedException extends RuntimeException {

  public InvitationAlreadyProcessedException() {
    super("This invitation has already been processed");
  }

    public InvitationAlreadyProcessedException(String message) {
        super(message);
    }
}

package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByEmailRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;

import java.util.List;

public interface GetPendingInvitationsByEmailUseCase {
    List<PetInvitationResponse> execute(GetPendingInvitationsByEmailRequest request);
}

package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.AcceptInvitationResponse;

public interface AcceptInvitationUseCase {
    AcceptInvitationResponse execute(AcceptInvitationRequest request);
}

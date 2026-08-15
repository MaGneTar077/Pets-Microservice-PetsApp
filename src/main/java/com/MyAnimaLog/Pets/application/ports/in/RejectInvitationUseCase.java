package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.RejectInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.RejectInvitationResponse;

public interface RejectInvitationUseCase {
    RejectInvitationResponse execute(RejectInvitationRequest request);
}

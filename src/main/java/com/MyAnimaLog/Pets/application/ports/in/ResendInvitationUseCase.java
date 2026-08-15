package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.ResendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.ResendInvitationResponse;

public interface ResendInvitationUseCase {
    ResendInvitationResponse execute(ResendInvitationRequest request);
}
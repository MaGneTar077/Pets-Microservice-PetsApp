package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.SendInvitationRequest;
import com.MyAnimaLog.Pets.application.dto.SendInvitationResponse;

public interface SendInvitationUseCase {
    SendInvitationResponse execute(SendInvitationRequest request);
}

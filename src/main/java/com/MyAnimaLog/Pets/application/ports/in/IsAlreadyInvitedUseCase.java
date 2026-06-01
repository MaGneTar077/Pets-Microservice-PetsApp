package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedRequest;
import com.MyAnimaLog.Pets.application.dto.IsAlreadyInvitedResponse;

public interface IsAlreadyInvitedUseCase {
    IsAlreadyInvitedResponse execute(IsAlreadyInvitedRequest request);
}
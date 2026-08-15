package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.RevokeAccessRequest;
import com.MyAnimaLog.Pets.application.dto.RevokeAccessResponse;

public interface RevokeAccessUseCase {
    RevokeAccessResponse execute(RevokeAccessRequest request);
}

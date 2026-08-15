package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;

public interface IsOwnerUseCase {
    IsOwnerResponse execute(IsOwnerRequest request);
}

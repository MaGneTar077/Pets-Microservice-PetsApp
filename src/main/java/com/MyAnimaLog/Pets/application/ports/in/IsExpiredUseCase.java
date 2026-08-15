package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.IsExpiredRequest;
import com.MyAnimaLog.Pets.application.dto.IsExpiredResponse;

public interface IsExpiredUseCase {
    IsExpiredResponse execute(IsExpiredRequest request);
}

package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.HasPermissionRequest;
import com.MyAnimaLog.Pets.application.dto.HasPermissionResponse;

public interface HasPermissionUseCase {
    HasPermissionResponse execute(HasPermissionRequest request);
}

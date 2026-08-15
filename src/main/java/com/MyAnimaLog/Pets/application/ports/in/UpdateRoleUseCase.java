package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.UpdateRoleRequest;
import com.MyAnimaLog.Pets.application.dto.UpdateRoleResponse;

public interface UpdateRoleUseCase {
    UpdateRoleResponse execute(UpdateRoleRequest request);
}

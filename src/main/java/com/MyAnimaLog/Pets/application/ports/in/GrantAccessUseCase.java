package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GrantAccessRequest;
import com.MyAnimaLog.Pets.application.dto.GrantAccessResponse;

public interface GrantAccessUseCase {
    GrantAccessResponse execute(GrantAccessRequest request);
}

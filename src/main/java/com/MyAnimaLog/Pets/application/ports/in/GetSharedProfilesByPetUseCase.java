package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesByPetRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;

import java.util.List;

public interface GetSharedProfilesByPetUseCase {
    List<SharedProfileResponse> execute(GetSharedProfilesByPetRequest request);
}

package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;

import java.util.List;

public interface GetSharedProfilesUseCase {
    List<SharedProfileResponse> execute(GetSharedProfilesRequest request);
}
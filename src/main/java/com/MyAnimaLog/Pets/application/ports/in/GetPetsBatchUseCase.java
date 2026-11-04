package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GetPetsBatchRequest;
import com.MyAnimaLog.Pets.application.dto.InternalPetResponse;

import java.util.List;

public interface GetPetsBatchUseCase {
    List<InternalPetResponse> execute(GetPetsBatchRequest request);
}

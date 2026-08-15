package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.AgeResponse;

import java.util.UUID;

public interface CalculateAgeUseCase {
    AgeResponse calculateAge(UUID petId);
}

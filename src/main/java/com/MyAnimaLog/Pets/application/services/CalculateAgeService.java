package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.AgeResponse;
import com.MyAnimaLog.Pets.application.ports.in.CalculateAgeUseCase;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.exceptions.BirthDateNotRegisteredException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalculateAgeService implements CalculateAgeUseCase {

    private final PetRepositoryPort petRepositoryPort;

    @Override
    public AgeResponse calculateAge(UUID petId) {
        Pet pet = petRepositoryPort.findById(petId)
                .orElseThrow(PetNotFoundException::new);

        if (pet.getBirthDate() == null) {
            throw new BirthDateNotRegisteredException();
        }

        Period period = Period.between(pet.getBirthDate(), LocalDate.now());

        return AgeResponse.builder()
                .years(period.getYears())
                .months(period.getMonths())
                .days(period.getDays())
                .build();
    }
}

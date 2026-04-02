package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.domain.model.Pet;

public interface AddPetUseCase {
    Pet addPet(Pet pet);
}

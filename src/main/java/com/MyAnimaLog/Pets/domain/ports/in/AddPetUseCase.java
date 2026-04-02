package com.MyAnimaLog.Pets.domain.ports.in;

import com.MyAnimaLog.Pets.domain.model.Pet;

public interface AddPetUseCase {
    Pet addPet(Pet pet);
}

package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.PetEvent;

public interface PublishPetEventUseCase {
    void publish(PetEvent event);
}

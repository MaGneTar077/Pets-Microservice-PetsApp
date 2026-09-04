package com.MyAnimaLog.Pets.application.ports.out;

import com.MyAnimaLog.Pets.application.dto.PetEvent;

public interface EventPublisherPort {
    void publishPetEvent(PetEvent event);
}

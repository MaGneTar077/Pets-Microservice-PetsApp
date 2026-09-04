package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.ports.in.PublishPetEventUseCase;
import com.MyAnimaLog.Pets.application.ports.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublishPetEventService implements PublishPetEventUseCase {

    private final EventPublisherPort eventPublisherPort;

    @Override
    public void publish(PetEvent event) {
        eventPublisherPort.publishPetEvent(event);
    }
}

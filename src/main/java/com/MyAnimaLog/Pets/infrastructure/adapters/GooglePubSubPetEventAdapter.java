package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.dto.PetEvent;
import com.MyAnimaLog.Pets.application.ports.out.EventPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePubSubPetEventAdapter implements EventPublisherPort {

    private static final Map<String, String> TOPIC_BY_EVENT_TYPE = Map.of(
            "PET_ADDED", "pet-added",
            "PET_EDITED", "pet-edited",
            "PET_PHOTO_UPLOADED", "pet-photo-uploaded",
            "PET_DOCUMENT_UPLOADED", "pet-document-uploaded",
            "PET_ROLE_UPDATED", "pet-role-updated",
            "PET_INVITATION_SENT", "pet-invitation-sent",
            "PET_INVITATION_ACCEPTED", "pet-invitation-accepted",
            "PET_INVITATION_REJECTED", "pet-invitation-rejected",
            "PET_INVITATION_RESENT", "pet-invitation-resent",
            "PET_SHARED", "pet-shared"
    );

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishPetEvent(PetEvent event) {
        String topic = TOPIC_BY_EVENT_TYPE.get(event.getEventType());

        if (topic == null) {
            log.error("No hay topic configurado para eventType={}, no se publica", event.getEventType());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            pubSubTemplate.publish(topic, payload);
            log.info("Evento {} publicado en topic {} para petId={}",
                    event.getEventType(), topic, event.getPetId());
        } catch (JsonProcessingException e) {
            log.error("Error serializando PetEvent para petId={}", event.getPetId(), e);
        }
    }
}

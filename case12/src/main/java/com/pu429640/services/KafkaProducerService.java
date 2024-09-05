package com.pu429640.services;

import com.pu429640.domain.UserTagEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private static final String TOPIC = "usertagevents";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserTagEvent(UserTagEvent userTagEvent) {
        this.kafkaTemplate.send(TOPIC, userTagEvent.getOrigin(), userTagEvent);
    }
}

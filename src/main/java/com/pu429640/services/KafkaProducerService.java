package com.pu429640.services;

import com.pu429640.domain.UserTagEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "usertagevents";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserTagEvent(UserTagEvent userTagEvent) {
        kafkaTemplate.send(TOPIC, userTagEvent);
    }
}
package com.user_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;



@Service
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserDeletedEvent(String topic, Object event) {
        kafkaTemplate.send(topic, event);
    }
}
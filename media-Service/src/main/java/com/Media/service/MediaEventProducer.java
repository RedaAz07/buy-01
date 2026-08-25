package com.Media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.Media.dto.response.AvatarUploadedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendAvatarUploadedEvent(String userId, String avatarUrl) {
        AvatarUploadedEvent event = new AvatarUploadedEvent(userId, avatarUrl);
        log.info("Publishing AvatarUploadedEvent for userId: {}", userId);
        kafkaTemplate.send("avatar-uploaded-topic", userId, event);
    }
}
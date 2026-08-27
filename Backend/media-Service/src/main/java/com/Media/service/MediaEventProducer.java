package com.Media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.Media.dto.response.AvatarUploadedEvent;
import com.Media.dto.response.MediaUploadedEvent;

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

    public void sendMediaUploadedEvent(String productId, String imageUrl) {
        MediaUploadedEvent event = new MediaUploadedEvent(productId, imageUrl);
        log.info("Publishing MediaUploadedEvent for productId: {}", productId);
        kafkaTemplate.send("media-uploaded-topic", productId, event);
    }
}
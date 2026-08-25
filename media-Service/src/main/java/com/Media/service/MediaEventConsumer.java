package com.Media.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.Media.dto.response.UserDeletedEvent;
import com.Media.repository.MediaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventConsumer {
    private final MediaRepository mediaRepository;

    @KafkaListener(topics = "user-deleted-topic", groupId = "media-service-group")
    public void handleUserDeleted(UserDeletedEvent event) {
        log.info("Kafka Event Received: Deleting all media for ownerId: {}", event.userId());
        mediaRepository.deleteByOwnerId(event.userId());
    }
}